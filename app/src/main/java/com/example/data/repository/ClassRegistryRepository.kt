package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.R
import com.example.auth.GoogleAuthHelper
import com.example.data.model.BranchStatus
import com.example.data.model.City
import com.example.data.model.ClassRoom
import com.example.data.model.ClassRoomDocument
import com.example.data.model.LinkedParentAccount
import com.example.data.model.School
import com.example.data.model.SchoolDocument
import com.example.data.model.Student
import com.example.data.model.UserRole
import com.example.util.AppDataWipeHelper
import com.example.util.ChatVisibilityHelper
import com.example.util.OperationType
import com.example.util.SmartBoardHelper
import com.example.util.TurkishTextHelper
import com.example.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.auth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.snapshots
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class ClassRegistryRepository(
    private val firestoreInstance: FirebaseFirestore? = null,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private val db: FirebaseFirestore
        get() = firestoreInstance ?: FirebaseFirestore.getInstance()

    // Secondary constructor using centralized FirestoreProvider
    constructor(context: Context) : this(
        try {
            com.example.util.FirestoreProvider.get(context)
        } catch (_: Exception) {
            null
        }
    )

    val cities: List<City> = TurkeyGeoRepository.allCities

    private val _schools = MutableStateFlow<List<School>>(emptyList())
    val schools: StateFlow<List<School>> = _schools.asStateFlow()

    private val _registeredClasses = MutableStateFlow<List<ClassRoom>>(emptyList())
    val registeredClasses: StateFlow<List<ClassRoom>> = _registeredClasses.asStateFlow()

    val allSections: List<String> = ('A'..'Z').map { it.toString() }

    private var classroomsJob: kotlinx.coroutines.Job? = null
    private var schoolsJob: kotlinx.coroutines.Job? = null

    @Volatile
    var isAccountDeleting: Boolean = false
        private set

    init {
        try {
            Firebase.auth.addAuthStateListener { auth ->
                val user = auth.currentUser
                if (user != null && !isAccountDeleting && !isGlobalAccountDeleting) {
                    startObserving()
                } else if (user == null) {
                    stopObserving()
                }
            }
            if (Firebase.auth.currentUser != null && !isAccountDeleting && !isGlobalAccountDeleting) {
                startObserving()
            }
        } catch (_: Exception) {}
    }

    private fun startObserving() {
        if (isAccountDeleting || isGlobalAccountDeleting) return
        if (classroomsJob == null || classroomsJob?.isActive == false) {
            classroomsJob = db.collection("classrooms")
                .snapshots()
                .onEach { snapshot ->
                    val list = snapshot.documents.mapNotNull { doc ->
                        val cityName = doc.getString("cityName") ?: return@mapNotNull null
                        val districtName = doc.getString("districtName") ?: return@mapNotNull null
                        val schoolName = doc.getString("schoolName") ?: return@mapNotNull null
                        val gradeLevel = (doc.getLong("gradeLevel") ?: 1L).toInt()
                        val section = doc.getString("section") ?: return@mapNotNull null
                        val teacherId = doc.getString("teacherId") ?: ""
                        val teacherName = doc.getString("teacherName") ?: ""
                        val teacherEmail = doc.getString("teacherEmail") ?: ""
                        val inviteCode = doc.getString("inviteCode") ?: ""
                        val teacherIsOnline = doc.getBoolean("teacherIsOnline") ?: false
                        val teacherLastActiveMillis = doc.getLong("teacherLastActiveMillis")

                        ClassRoom(
                            id = doc.id,
                            cityName = cityName,
                            districtName = districtName,
                            schoolName = schoolName,
                            gradeLevel = gradeLevel,
                            section = section,
                            teacherId = teacherId,
                            teacherName = teacherName,
                            teacherEmail = teacherEmail,
                            inviteCode = inviteCode,
                            isOccupied = true,
                            teacherIsOnline = teacherIsOnline,
                            teacherLastActiveMillis = teacherLastActiveMillis
                        )
                    }
                    _registeredClasses.value = list
                }
                .catch { error ->
                    if (error is Exception) {
                        handleFirestoreError(error, OperationType.LIST, "classrooms")
                    }
                }
                .launchIn(scope)
        }

        if (schoolsJob == null || schoolsJob?.isActive == false) {
            schoolsJob = db.collection("schools")
                .snapshots()
                .onEach { snapshot ->
                    val list = snapshot.documents.mapNotNull { doc ->
                        val cityId = doc.getString("cityId") ?: return@mapNotNull null
                        val districtName = doc.getString("districtName") ?: return@mapNotNull null
                        val name = doc.getString("name") ?: return@mapNotNull null

                        School(
                            id = doc.id,
                            name = name,
                            cityName = doc.getString("cityName") ?: "",
                            districtName = districtName,
                            cityId = cityId
                        )
                    }
                    _schools.value = list
                }
                .catch { error ->
                    if (error is Exception) {
                        handleFirestoreError(error, OperationType.LIST, "schools")
                    }
                }
                .launchIn(scope)
        }
    }

    private fun stopObserving() {
        classroomsJob?.cancel()
        classroomsJob = null
        schoolsJob?.cancel()
        schoolsJob = null
        _registeredClasses.value = emptyList()
        _schools.value = emptyList()
    }

    var isTestMode: Boolean = false
        private set
    var testUserId: String? = null
        private set

    fun enableTestMode(userId: String = "test_teacher_preview_uid") {
        isTestMode = true
        testUserId = userId
    }

    fun disableTestMode() {
        isTestMode = false
        testUserId = null
    }

    fun stopAllListenersForAccountDeletion() {
        isAccountDeleting = true
        isGlobalAccountDeleting = true
        stopObserving()
        studentsJob?.cancel()
        studentsJob = null
        _students.value = emptyList()
        _registeredClasses.value = emptyList()
    }

    fun clearSessionCaches() {
        stopObserving()
        studentsJob?.cancel()
        studentsJob = null
        _students.value = emptyList()
        _registeredClasses.value = emptyList()
        disableTestMode()
    }

    val defaultSampleClass: ClassRoom = ClassRoom(
        id = "58_merkez_suleyman_sami_kepenek_ilkokulu_4d",
        cityName = "SİVAS",
        districtName = "MERKEZ",
        schoolName = "SÜLEYMAN SAMİ KEPENEK İLKOKULU",
        gradeLevel = 4,
        section = "D",
        teacherName = "Cihan Özel",
        teacherEmail = "cihan.ozel10@gmail.com",
        inviteCode = "BRZ-4D-5844",
        studentCount = 25,
        isOccupied = true
    )

    val defaultSampleStudents: List<Student> = listOf(
        Student(
            id = "std_45",
            studentNumber = "45",
            fullName = "SEMİH AHMET KAVAKOĞLU",
            gender = "Erkek",
            parentInviteCode = "94K7P2MN",
            parentName = "Ahmet Kavakoğlu",
            parentEmail = "ahmet.kav@gmail.com",
            parentLinkedAt = "12 Ekim 2026, 14:20",
            parentLastLoginAt = "12 Ekim 2026, 14:20",
            linkedParents = listOf(
                LinkedParentAccount(
                    email = "ahmet.kav@gmail.com",
                    name = "Ahmet Kavakoğlu",
                    phone = "0532 111 22 33",
                    relationship = "Baba",
                    linkedAt = "12 Ekim 2026, 14:20",
                    lastLoginAt = "12 Ekim 2026, 14:20"
                ),
                LinkedParentAccount(
                    email = "zeynep.kav@gmail.com",
                    name = "Zeynep Kavakoğlu",
                    phone = "0533 222 33 44",
                    relationship = "Anne",
                    linkedAt = "14 Ekim 2026, 11:05",
                    lastLoginAt = "14 Ekim 2026, 11:05"
                )
            )
        ),
        Student(id = "std_326", studentNumber = "326", fullName = "GÜNEŞ DEMİR", gender = "Kız", parentInviteCode = "7M3K8Q2P", isPriority = true),
        Student(id = "std_402", studentNumber = "402", fullName = "AHMET ASAF IŞIK", gender = "Erkek", parentInviteCode = "TD8GDLU8"),
        Student(id = "std_403", studentNumber = "403", fullName = "AYŞE MİRAY BORHAN", gender = "Kız", parentInviteCode = "4K8M2P9R"),
        Student(id = "std_405", studentNumber = "405", fullName = "BAHATTİN SÜER", gender = "Erkek", parentInviteCode = "7N3Q6T8W"),
        Student(id = "std_406", studentNumber = "406", fullName = "DEFNE YARAMIŞ", gender = "Kız", parentInviteCode = "2R9V4X7Z"),
        Student(id = "std_407", studentNumber = "407", fullName = "EMİR ASAF AKKAŞ", gender = "Erkek", parentInviteCode = "8K3N9P2X"),
        Student(
            id = "std_408",
            studentNumber = "408",
            fullName = "KEVSER ÖZDEMİR",
            gender = "Kız",
            parentInviteCode = "5H2L9R4T",
            parentName = "Fatma Özdemir",
            parentEmail = "fatma.ozdemir@gmail.com",
            parentLinkedAt = "18 Ekim 2026, 10:00",
            parentLastLoginAt = "18 Ekim 2026, 10:00",
            linkedParents = listOf(
                LinkedParentAccount(
                    email = "fatma.ozdemir@gmail.com",
                    name = "Fatma Özdemir",
                    phone = "0555 333 44 55",
                    relationship = "Anne",
                    linkedAt = "18 Ekim 2026, 10:00",
                    lastLoginAt = "18 Ekim 2026, 10:00"
                )
            )
        ),
        Student(id = "std_409", studentNumber = "409", fullName = "MEHMET AKİF KILAVUZ", gender = "Erkek", parentInviteCode = "3V7W2X8Y"),
        Student(id = "std_411", studentNumber = "411", fullName = "MELİSA YÜCE", gender = "Kız", parentInviteCode = "6B4N9M1K"),
        Student(id = "std_413", studentNumber = "413", fullName = "MERT KARA", gender = "Erkek", parentInviteCode = "2P8Q5R3T"),
        Student(id = "std_414", studentNumber = "414", fullName = "METİN BARIŞ DUMAN", gender = "Erkek", parentInviteCode = "8M4K7P2N"),
        Student(id = "std_415", studentNumber = "415", fullName = "MİRA GÜLMEZ", gender = "Kız", parentInviteCode = "9X1Y4Z7W"),
        Student(id = "std_416", studentNumber = "416", fullName = "MUHAMMED ERTUĞRUL AYDIN", gender = "Erkek", parentInviteCode = "5T9R3K7L"),
        Student(id = "std_418", studentNumber = "418", fullName = "ÖMER SAKİ ÇETİN", gender = "Erkek", parentInviteCode = "4M6N8P2R"),
        Student(id = "std_420", studentNumber = "420", fullName = "YİĞİT EFE ŞEN", gender = "Erkek", parentInviteCode = "7T3K9L5Q"),
        Student(id = "std_421", studentNumber = "421", fullName = "ZEHRA CEYLAN", gender = "Kız", parentInviteCode = "6Q9M2N4P"),
        Student(id = "std_422", studentNumber = "422", fullName = "ZEYNEP İKRA DENİZ", gender = "Kız", parentInviteCode = "5R8W2V4X"),
        Student(id = "std_425", studentNumber = "425", fullName = "CANSEL VAROL", gender = "Kız", parentInviteCode = "3N6P9Q1M"),
        Student(id = "std_426", studentNumber = "426", fullName = "ELİF YAZICI", gender = "Kız", parentInviteCode = "8K4L7R2T"),
        Student(id = "std_429", studentNumber = "429", fullName = "DORUK KİRAZ", gender = "Erkek", parentInviteCode = "4T7R1K8L"),
        Student(id = "std_430", studentNumber = "430", fullName = "ZEYNEPSU ÇALIŞKAN", gender = "Kız", parentInviteCode = "2W5Y8Z3V"),
        Student(id = "std_441", studentNumber = "441", fullName = "KÜRŞAD ADEM CEBECİ", gender = "Erkek", parentInviteCode = "9V2X5Y7Z"),
        Student(id = "std_444", studentNumber = "444", fullName = "ESİLA ATMACA", gender = "Kız", parentInviteCode = "3P6Q8R1M"),
        Student(id = "std_555", studentNumber = "555", fullName = "ARAS GÖKKAYA", gender = "Erkek", parentInviteCode = "7K4L9N2T")
    )

    fun findInSampleStudents(
        sanitizedCode: String,
        currentUserEmail: String = "veli@biriz.app",
        currentUserFullName: String = "Öğrenci Velisi"
    ): Pair<Student, ClassRoom>? {
        val normTarget = TurkishTextHelper.normalizeVeliKatilimKodu(sanitizedCode)
        val match = defaultSampleStudents.firstOrNull { s ->
            val code = TurkishTextHelper.normalizeVeliKatilimKodu(s.parentInviteCode)
            val no = TurkishTextHelper.normalizeVeliKatilimKodu(s.studentNumber)
            val id = TurkishTextHelper.normalizeVeliKatilimKodu(s.id)
            code == normTarget || no == normTarget || id == normTarget
        } ?: return null

        val nowMillis = System.currentTimeMillis()
        val todayStr = java.text.SimpleDateFormat("dd MMMM yyyy, HH:mm", java.util.Locale("tr")).format(java.util.Date(nowMillis))
        val lastLoginStr = todayStr
        val finalEmail = currentUserEmail.ifBlank { "veli@biriz.app" }
        val finalName = currentUserFullName.ifBlank { "Öğrenci Velisi" }

        val newParentAccount = LinkedParentAccount(
            email = finalEmail,
            name = finalName,
            phone = "",
            relationship = "Veli",
            linkedAt = todayStr,
            lastLoginAt = lastLoginStr,
            isOnline = true,
            lastActiveMillis = nowMillis
        )
        val updatedParents = listOf(newParentAccount) + match.allLinkedParents.filterNot { it.email.equals(finalEmail, ignoreCase = true) }
        val updatedStudent = match.copy(
            parentName = finalName,
            parentEmail = finalEmail,
            parentLinkedAt = todayStr,
            parentLastLoginAt = lastLoginStr,
            parentIsOnline = true,
            parentLastActiveMillis = nowMillis,
            linkedParents = updatedParents
        )
        _students.value = _students.value.map { if (it.id == updatedStudent.id) updatedStudent else it }
        if (_students.value.none { it.id == updatedStudent.id }) {
            _students.value = defaultSampleStudents.map { if (it.id == updatedStudent.id) updatedStudent else it }
        }
        return Pair(updatedStudent, defaultSampleClass)
    }

    fun syncLinkedStudentsWithParent(
        linkedStudentIds: List<String>,
        parentEmail: String = "veli@biriz.app",
        parentName: String = "Öğrenci Velisi"
    ) {
        if (linkedStudentIds.isEmpty()) return
        val nowMillis = System.currentTimeMillis()
        val todayStr = java.text.SimpleDateFormat("dd MMMM yyyy, HH:mm", java.util.Locale("tr")).format(java.util.Date(nowMillis))
        val lastLoginStr = todayStr
        val finalEmail = parentEmail.ifBlank { "veli@biriz.app" }
        val finalName = parentName.ifBlank { "Öğrenci Velisi" }

        val account = LinkedParentAccount(
            email = finalEmail,
            name = finalName,
            phone = "",
            relationship = "Veli",
            linkedAt = todayStr,
            lastLoginAt = lastLoginStr,
            isOnline = true,
            lastActiveMillis = nowMillis
        )

        _students.value = _students.value.map { s ->
            if (s.id in linkedStudentIds) {
                val hasThisParent = s.allLinkedParents.any { it.email.equals(finalEmail, ignoreCase = true) }
                if (!hasThisParent) {
                    val updatedParents = listOf(account) + s.allLinkedParents.filterNot { it.email.equals(finalEmail, ignoreCase = true) }
                    s.copy(
                        parentName = s.parentName ?: finalName,
                        parentEmail = s.parentEmail ?: finalEmail,
                        parentLinkedAt = s.parentLinkedAt ?: todayStr,
                        parentLastLoginAt = lastLoginStr,
                        parentIsOnline = true,
                        parentLastActiveMillis = nowMillis,
                        linkedParents = updatedParents
                    )
                } else s
            } else s
        }
    }

    suspend fun findAndLinkStudentByCode(
        sanitizedCode: String,
        currentUserEmail: String,
        currentUserFullName: String
    ): Pair<Student, ClassRoom?>? {
        val normTarget = TurkishTextHelper.normalizeVeliKatilimKodu(sanitizedCode)
        if (normTarget.isBlank()) return null

        val nowMillis = System.currentTimeMillis()
        val todayStr = java.text.SimpleDateFormat("dd MMMM yyyy, HH:mm", java.util.Locale("tr")).format(java.util.Date(nowMillis))
        val lastLoginStr = todayStr
        val finalEmail = currentUserEmail.ifBlank { "veli@biriz.app" }
        val finalName = currentUserFullName.ifBlank { "Öğrenci Velisi" }

        val newParentAccount = LinkedParentAccount(
            email = finalEmail,
            name = finalName,
            phone = "",
            relationship = "Veli",
            linkedAt = todayStr,
            lastLoginAt = lastLoginStr,
            isOnline = true,
            lastActiveMillis = nowMillis
        )

        // 1. Önce hafızadaki öğrencilerden kontrol et (eğer sınıf zaten yüklüyse)
        val inMemoryMatch = _students.value.firstOrNull { s ->
            val sc = TurkishTextHelper.normalizeVeliKatilimKodu(s.parentInviteCode)
            val no = TurkishTextHelper.normalizeVeliKatilimKodu(s.studentNumber)
            val id = TurkishTextHelper.normalizeVeliKatilimKodu(s.id)
            sc == normTarget || no == normTarget || id == normTarget
        }
        if (inMemoryMatch != null) {
            val matchedClass = _registeredClasses.value.firstOrNull() ?: defaultSampleClass
            val updatedParents = listOf(newParentAccount) + inMemoryMatch.allLinkedParents.filterNot { it.email.equals(finalEmail, ignoreCase = true) }
            val updatedStudent = inMemoryMatch.copy(
                parentName = finalName,
                parentEmail = finalEmail,
                parentLinkedAt = todayStr,
                parentLastLoginAt = lastLoginStr,
                parentIsOnline = true,
                parentLastActiveMillis = nowMillis,
                linkedParents = updatedParents
            )

            // Hem hafızadaki _students StateFlow'unu hem de varsa Firestore'u anında senkronize et
            _students.value = _students.value.map { if (it.id == updatedStudent.id) updatedStudent else it }

            if (Firebase.auth.currentUser != null) {
                try {
                    val classId = matchedClass.id
                    val sRef = db.collection("classrooms").document(classId).collection("students").document(updatedStudent.id)
                    val updatePayload = mapOf<String, Any?>(
                        "parentCode" to updatedStudent.parentInviteCode,
                        "parentInviteCode" to updatedStudent.parentInviteCode,
                        "parentEmail" to finalEmail,
                        "parentName" to finalName,
                        "parentLinkedAt" to todayStr,
                        "parentLastLoginAt" to lastLoginStr,
                        "parentIsOnline" to true,
                        "parentLastActiveMillis" to nowMillis,
                        "linkedParents" to updatedParents.map { p ->
                            mapOf(
                                "email" to p.email,
                                "name" to p.name,
                                "phone" to p.phone,
                                "relationship" to p.relationship,
                                "linkedAt" to p.linkedAt,
                                "lastLoginAt" to p.lastLoginAt,
                                "isOnline" to p.isOnline,
                                "lastActiveMillis" to p.lastActiveMillis
                            )
                        },
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    sRef.update(updatePayload).await()
                } catch (_: Exception) {}
            }
            return Pair(updatedStudent, matchedClass)
        }

        // 2. Firestore üzerinde tüm sınıfları ve altındaki öğrencileri paralel ara
        if (Firebase.auth.currentUser != null) {
            try {
                val classSnap = withTimeoutOrNull(5000L) {
                    db.collection("classrooms").get().await()
                }
                if (classSnap != null) {
                    val matchedPair = coroutineScope {
                        classSnap.documents.map { cDoc ->
                            async {
                                val classId = cDoc.id
                                val studentsSnap = withTimeoutOrNull(4500L) {
                                    db.collection("classrooms").document(classId)
                                        .collection("students").get().await()
                                } ?: return@async null

                                for (sDoc in studentsSnap.documents) {
                                    val studentNo = (sDoc.getLong("studentNo") ?: 0L).toInt()
                                    val fullName = sDoc.getString("fullName") ?: ""
                                    val gender = sDoc.getString("gender") ?: "Erkek"
                                    val rawCode = sDoc.getString("parentCode")
                                        ?: sDoc.getString("parentInviteCode")
                                        ?: sDoc.getString("veliKodu")
                                        ?: sDoc.getString("code")
                                    val normCode = TurkishTextHelper.normalizeVeliKatilimKodu(rawCode ?: "")
                                    val sNoStr = studentNo.toString()

                                    val isMatch = (normCode.isNotBlank() && normCode == normTarget) ||
                                        (sNoStr == normTarget) ||
                                        (sDoc.id == normTarget)

                                    if (isMatch) {
                                        val finalCode = if (normCode.isNotBlank()) normCode else normTarget
                                        val existingParents = parseLinkedParentsFromDoc(sDoc, includeLegacyFallback = true)
                                        val updatedParents = listOf(newParentAccount) + existingParents.filterNot {
                                            it.email.equals(finalEmail, ignoreCase = true)
                                        }
                                        val updatePayload = mapOf<String, Any?>(
                                            "parentCode" to finalCode,
                                            "parentInviteCode" to finalCode,
                                            "parentEmail" to finalEmail,
                                            "parentName" to finalName,
                                            "parentLinkedAt" to todayStr,
                                            "parentLastLoginAt" to lastLoginStr,
                                            "parentIsOnline" to true,
                                            "parentLastActiveMillis" to nowMillis,
                                            "linkedParents" to updatedParents.map { p ->
                                                mapOf(
                                                    "email" to p.email,
                                                    "name" to p.name,
                                                    "phone" to p.phone,
                                                    "relationship" to p.relationship,
                                                    "linkedAt" to p.linkedAt,
                                                    "lastLoginAt" to p.lastLoginAt,
                                                    "isOnline" to p.isOnline,
                                                    "lastActiveMillis" to p.lastActiveMillis
                                                )
                                            },
                                            "updatedAt" to FieldValue.serverTimestamp()
                                        )
                                        try {
                                            sDoc.reference.update(updatePayload).await()
                                        } catch (_: Exception) {}

                                        val classRoom = ClassRoom(
                                            id = cDoc.id,
                                            cityName = cDoc.getString("cityName") ?: "",
                                            districtName = cDoc.getString("districtName") ?: "",
                                            schoolName = cDoc.getString("schoolName") ?: "",
                                            gradeLevel = (cDoc.getLong("gradeLevel") ?: 1L).toInt(),
                                            section = cDoc.getString("section") ?: "A",
                                            teacherName = cDoc.getString("teacherName") ?: "",
                                            teacherEmail = cDoc.getString("teacherEmail") ?: "",
                                            inviteCode = cDoc.getString("inviteCode") ?: "",
                                            isOccupied = true
                                        )

                                        val student = Student(
                                            id = sDoc.id,
                                            studentNumber = sNoStr,
                                            fullName = fullName,
                                            gender = gender,
                                            parentInviteCode = finalCode,
                                            parentName = finalName,
                                            parentEmail = finalEmail,
                                            parentLinkedAt = todayStr,
                                            parentLastLoginAt = lastLoginStr,
                                            parentIsOnline = true,
                                            parentLastActiveMillis = nowMillis,
                                            linkedParents = updatedParents
                                        )
                                        return@async Pair(student, classRoom)
                                    }
                                }
                                null
                            }
                        }.awaitAll().firstOrNull { it != null }
                    }

                    if (matchedPair != null) {
                        val (student, classRoom) = matchedPair
                        _students.value = _students.value.map { if (it.id == student.id) student else it }
                        if (_students.value.none { it.id == student.id }) {
                            _students.value = _students.value + student
                        }
                        observeStudents(classRoom.id)
                        return matchedPair
                    }
                }
            } catch (_: Exception) {}
        }

        // 3. Bulunamazsa örnek öğrenci listesinden kontrol et
        return findInSampleStudents(normTarget, currentUserEmail, currentUserFullName)
    }

    fun seedDefaultTestClassroom(userId: String = "test_teacher_preview_uid"): ClassRoom {
        _students.value = defaultSampleStudents
        _registeredClasses.value = _registeredClasses.value.filter { it.id != defaultSampleClass.id } + defaultSampleClass
        return defaultSampleClass
    }

    private fun requireUserId(): String {
        val fbUid = Firebase.auth.currentUser?.uid
        if (fbUid != null) return fbUid
        if (isTestMode) return testUserId ?: "test_teacher_preview_uid"
        throw IllegalStateException("İşlem için Google ile oturum açılmış olmalıdır.")
    }

    fun getBranchStatuses(
        cityName: String,
        districtName: String,
        schoolName: String,
        gradeLevel: Int
    ): List<BranchStatus> {
        val occupiedInSchool = _registeredClasses.value.filter {
            it.cityName.equals(cityName.trim(), ignoreCase = true) &&
            it.districtName.equals(districtName.trim(), ignoreCase = true) &&
            it.schoolName.equals(schoolName.trim(), ignoreCase = true) &&
            it.gradeLevel == gradeLevel
        }

        val occupiedMap = occupiedInSchool.associateBy { it.section.uppercase() }

        return allSections.map { sectionChar ->
            val occupiedClass = occupiedMap[sectionChar]
            BranchStatus(
                section = sectionChar,
                isOccupied = occupiedClass != null,
                assignedTeacherName = occupiedClass?.teacherName,
                assignedTeacherEmail = occupiedClass?.teacherEmail
            )
        }
    }

    fun checkConflict(
        cityName: String,
        districtName: String,
        schoolName: String,
        gradeLevel: Int,
        section: String
    ): ClassRoom? {
        return _registeredClasses.value.firstOrNull {
            it.cityName.equals(cityName.trim(), ignoreCase = true) &&
            it.districtName.equals(districtName.trim(), ignoreCase = true) &&
            it.schoolName.equals(schoolName.trim(), ignoreCase = true) &&
            it.gradeLevel == gradeLevel &&
            it.section.equals(section.trim(), ignoreCase = true)
        }
    }

    suspend fun addSchool(
        cityId: String,
        districtName: String,
        schoolName: String
    ): School {
        val uid = requireUserId()
        val normalizedName = TurkishTextHelper.toTurkishUpperCase(schoolName.trim())
        val docId = "${cityId}_${districtName.lowercase().replace(" ", "_")}_${normalizedName.lowercase().replace(" ", "_")}"
            .filter { it.isLetterOrDigit() || it == '_' }

        val cityName = TurkeyGeoRepository.allCities.firstOrNull { it.id == cityId }?.name ?: ""
        val newSchool = School(
            id = docId,
            name = normalizedName,
            cityName = cityName,
            districtName = districtName,
            cityId = cityId
        )

        if (Firebase.auth.currentUser != null) {
            val docRef = db.collection("schools").document(docId)
            val payload = mapOf(
                "cityId" to cityId,
                "districtName" to TurkishTextHelper.toTurkishUpperCase(districtName),
                "name" to normalizedName,
                "creatorId" to uid,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            try {
                docRef.set(payload).await()
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.CREATE, docRef.path)
                throw e
            }
        }

        if (_schools.value.none { it.id == docId }) {
            _schools.value = _schools.value + newSchool
        }

        return newSchool
    }

    suspend fun registerClass(
        city: City,
        districtName: String,
        schoolName: String,
        gradeLevel: Int,
        section: String,
        teacherName: String,
        teacherEmail: String
    ): ClassRoom {
        val uid = requireUserId()
        val normalizedSchool = TurkishTextHelper.toTurkishUpperCase(schoolName.trim())
        val normalizedSection = section.uppercase().trim()
        val normalizedCity = TurkishTextHelper.toTurkishUpperCase(city.name)
        val normalizedDistrict = TurkishTextHelper.toTurkishUpperCase(districtName)

        val docId = "${city.id}_${districtName.lowercase().replace(" ", "_")}_${normalizedSchool.lowercase().replace(" ", "_")}_${gradeLevel}${normalizedSection.lowercase()}"
            .filter { it.isLetterOrDigit() || it == '_' }

        val existingConflict = checkConflict(
            cityName = normalizedCity,
            districtName = normalizedDistrict,
            schoolName = normalizedSchool,
            gradeLevel = gradeLevel,
            section = normalizedSection
        )
        if (existingConflict != null) {
            throw IllegalStateException(
                "Bu şube (${normalizedCity} / ${normalizedSchool} / ${gradeLevel}-${normalizedSection}) zaten ${existingConflict.teacherName} adlı öğretmene atanmıştır!"
            )
        }

        val inviteCode = "BRZ-${gradeLevel}${normalizedSection}-${city.id}${((1000..9999).random())}"

        val classRoom = ClassRoom(
            id = docId,
            cityName = normalizedCity,
            districtName = normalizedDistrict,
            schoolName = normalizedSchool,
            gradeLevel = gradeLevel,
            section = normalizedSection,
            teacherId = uid,
            teacherName = teacherName,
            teacherEmail = teacherEmail,
            inviteCode = inviteCode,
            isOccupied = true
        )

        if (Firebase.auth.currentUser != null) {
            val classroomPayload = mapOf(
                "cityId" to city.id,
                "cityName" to normalizedCity,
                "districtName" to normalizedDistrict,
                "schoolName" to normalizedSchool,
                "gradeLevel" to gradeLevel,
                "section" to normalizedSection,
                "teacherId" to uid,
                "teacherName" to teacherName,
                "teacherEmail" to teacherEmail,
                "inviteCode" to inviteCode,
                "studentCount" to 0,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            val docRef = db.collection("classrooms").document(docId)
            try {
                docRef.set(classroomPayload).await()
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.CREATE, docRef.path)
                throw e
            }

            // Also update user profile to assign class
            if (!isAccountDeleting && !isGlobalAccountDeleting) {
                val userRef = db.collection("users").document(uid)
                val userPayload = mapOf(
                    "userId" to uid,
                    "fullName" to teacherName,
                    "email" to teacherEmail,
                    "role" to "TEACHER",
                    "kvkkAccepted" to true,
                    "assignedClassId" to docId,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                try {
                    userRef.set(userPayload, com.google.firebase.firestore.SetOptions.merge()).await()
                } catch (e: Exception) {
                    handleFirestoreError(e, OperationType.UPDATE, userRef.path)
                }
            }
        }

        _registeredClasses.value = _registeredClasses.value.filter { it.id != docId } + classRoom
        return classRoom
    }

    // --- Öğrenci Listesi Yönetimi (Firestore Subcollection) ---

    private val _students = MutableStateFlow<List<Student>>(emptyList())
    val students: StateFlow<List<Student>> = _students.asStateFlow()

    private var studentsJob: kotlinx.coroutines.Job? = null
    private val backfilledCodeStudentIds = java.util.Collections.synchronizedSet(mutableSetOf<String>())

    fun observeStudents(classroomId: String) {
        studentsJob?.cancel()
        if (isAccountDeleting || isGlobalAccountDeleting) return
        if (Firebase.auth.currentUser != null) {
            studentsJob = db.collection("classrooms").document(classroomId).collection("students")
                .snapshots()
                .onEach { snapshot ->
                    val list = snapshot.documents.mapNotNull { doc ->
                        val studentNo = (doc.getLong("studentNo") ?: 0L).toInt()
                        val fullName = doc.getString("fullName") ?: return@mapNotNull null
                        val gender = doc.getString("gender") ?: "Erkek"
                        val rawCode = doc.getString("parentCode")
                            ?: doc.getString("parentInviteCode")
                            ?: doc.getString("veliKodu")
                            ?: doc.getString("code")
                        val parentCode = if (!rawCode.isNullOrBlank()) {
                            rawCode
                        } else {
                            val stable = TurkishTextHelper.generateVeliKatilimKodu()
                            val docKey = "$classroomId/${doc.id}"
                            if (backfilledCodeStudentIds.add(docKey)) {
                                scope.launch {
                                    try {
                                        doc.reference.update(
                                            mapOf(
                                                "parentCode" to stable,
                                                "parentInviteCode" to stable
                                            )
                                        ).await()
                                    } catch (_: Exception) {
                                        backfilledCodeStudentIds.remove(docKey)
                                    }
                                }
                            }
                            stable
                        }
                        val parentName = doc.getString("parentName")
                        val parentEmail = doc.getString("parentEmail")
                        val parentLinkedAt = doc.getString("parentLinkedAt")
                        val parentLastLoginAt = doc.getString("parentLastLoginAt")
                        val hasTopOnlineField = doc.contains("parentIsOnline")
                        val parentIsOnline = doc.getBoolean("parentIsOnline") ?: false
                        val parentLastActiveMillis = doc.getLong("parentLastActiveMillis")
                        @Suppress("UNCHECKED_CAST")
                        val rawLinkedParents = doc.get("linkedParents") as? List<Map<String, Any?>>
                        val parsedLinked = rawLinkedParents?.mapNotNull { map ->
                            val e = map["email"] as? String
                            val n = map["name"] as? String
                            if (!e.isNullOrBlank() && !n.isNullOrBlank()) {
                                val singleParent = rawLinkedParents.size <= 1
                                val pOnline = (map["isOnline"] as? Boolean)
                                    ?: if (hasTopOnlineField && singleParent) parentIsOnline else false
                                val pActiveMillis = (map["lastActiveMillis"] as? Number)?.toLong()
                                    ?: if (singleParent) parentLastActiveMillis else null
                                val pLastLogin = (map["lastLoginAt"] as? String)
                                    ?: if (singleParent) parentLastLoginAt else null
                                LinkedParentAccount(
                                    email = e,
                                    name = n,
                                    phone = (map["phone"] as? String) ?: "",
                                    relationship = (map["relationship"] as? String) ?: "Veli",
                                    linkedAt = (map["linkedAt"] as? String) ?: "Bağlandı",
                                    lastLoginAt = pLastLogin,
                                    isOnline = pOnline,
                                    lastActiveMillis = pActiveMillis
                                )
                            } else null
                        } ?: emptyList()

                        Student(
                            id = doc.id,
                            studentNumber = studentNo.toString(),
                            fullName = fullName,
                            gender = gender,
                            parentInviteCode = parentCode,
                            parentName = parentName,
                            parentEmail = parentEmail,
                            parentLinkedAt = parentLinkedAt,
                            parentLastLoginAt = parentLastLoginAt,
                            parentIsOnline = parentIsOnline,
                            parentLastActiveMillis = parentLastActiveMillis,
                            linkedParents = parsedLinked
                        )
                    }.sortedBy { it.studentNumber.toIntOrNull() ?: 9999 }
                    _students.value = list
                }
                .catch { error ->
                    if (error is Exception) {
                        handleFirestoreError(error, OperationType.LIST, "classrooms/$classroomId/students")
                    }
                }
                .launchIn(scope)
        }
    }

    suspend fun addStudent(
        classroomId: String,
        studentNo: Int,
        fullName: String,
        gender: String
    ): Student {
        val uid = requireUserId()
        val normalizedName = TurkishTextHelper.toTurkishUpperCase(fullName.trim())
        val parentCode = TurkishTextHelper.generateVeliKatilimKodu()
        val fallbackId = "std_${System.currentTimeMillis()}_$studentNo"

        // Okul numarası tekillik kontrolü
        if (_students.value.any { it.studentNumber == studentNo.toString() }) {
            throw IllegalArgumentException("$studentNo numaralı öğrenci bu sınıfta zaten kayıtlıdır!")
        }

        val student = Student(
            id = fallbackId,
            studentNumber = studentNo.toString(),
            fullName = normalizedName,
            gender = if (gender.equals("Kız", ignoreCase = true)) "Kız" else "Erkek",
            parentInviteCode = parentCode
        )

        if (Firebase.auth.currentUser != null) {
            val studentDocRef = db.collection("classrooms").document(classroomId).collection("students").document()
            val payload = mapOf(
                "studentNo" to studentNo,
                "fullName" to normalizedName,
                "gender" to student.gender,
                "parentCode" to parentCode,
                "classroomId" to classroomId,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            try {
                studentDocRef.set(payload).await()
                db.collection("classrooms").document(classroomId).update(
                    mapOf(
                        "studentCount" to FieldValue.increment(1),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.CREATE, studentDocRef.path)
                throw e
            }

            return student.copy(id = studentDocRef.id)
        } else {
            _students.value = (_students.value + student).sortedBy { it.studentNumber.toIntOrNull() ?: 9999 }
            return student
        }
    }

    suspend fun editStudent(
        classroomId: String,
        studentId: String,
        newStudentNo: Int,
        newFullName: String,
        newGender: String
    ) {
        val normalizedName = TurkishTextHelper.toTurkishUpperCase(newFullName.trim())
        val normalizedGender = if (newGender.equals("Kız", ignoreCase = true)) "Kız" else "Erkek"

        // Numara değiştiyse başka öğrencide var mı kontrol et
        if (_students.value.any { it.id != studentId && it.studentNumber == newStudentNo.toString() }) {
            throw IllegalArgumentException("$newStudentNo numaralı başka bir öğrenci zaten mevcuttur!")
        }

        if (Firebase.auth.currentUser != null) {
            val studentRef = db.collection("classrooms").document(classroomId).collection("students").document(studentId)
            val updates = mapOf(
                "studentNo" to newStudentNo,
                "fullName" to normalizedName,
                "gender" to normalizedGender,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            try {
                studentRef.update(updates).await()
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.UPDATE, studentRef.path)
                throw e
            }
        }

        _students.value = _students.value.map { current ->
            if (current.id == studentId) {
                current.copy(
                    studentNumber = newStudentNo.toString(),
                    fullName = normalizedName,
                    gender = normalizedGender
                )
            } else {
                current
            }
        }.sortedBy { it.studentNumber.toIntOrNull() ?: 9999 }
    }

    suspend fun addStudentsBatch(
        classroomId: String,
        studentsToAdd: List<Triple<Int, String, String>>
    ): Int {
        var addedCount = 0
        val existingNos = _students.value.map { it.studentNumber }.toMutableSet()

        for (item in studentsToAdd) {
            val (no, rawName, rawGender) = item
            if (existingNos.contains(no.toString())) {
                continue // zaten kayıtlı olanı atla
            }
            existingNos.add(no.toString())
            addStudent(classroomId, no, rawName, rawGender)
            addedCount++
        }
        return addedCount
    }

    suspend fun deleteStudent(classroomId: String, studentId: String) {
        if (Firebase.auth.currentUser != null) {
            val studentRef = db.collection("classrooms").document(classroomId).collection("students").document(studentId)
            try {
                studentRef.delete().await()
                db.collection("classrooms").document(classroomId).update(
                    mapOf(
                        "studentCount" to FieldValue.increment(-1),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.DELETE, studentRef.path)
                throw e
            }
        }
        _students.value = _students.value.filter { it.id != studentId }
    }

    suspend fun updateStudentParentInfo(
        classroomId: String,
        studentId: String,
        parentName: String?,
        parentEmail: String?,
        parentLinkedAt: String? = null,
        parentLastLoginAt: String? = null
    ) {
        val isDisconnecting = parentName == null && parentEmail == null
        val nowMillis = System.currentTimeMillis()
        if (isFirebaseAuthAvailable) {
            val studentRef = db.collection("classrooms").document(classroomId).collection("students").document(studentId)
            val updates = if (isDisconnecting) {
                mutableMapOf<String, Any?>(
                    "parentName" to null,
                    "parentEmail" to null,
                    "parentLinkedAt" to null,
                    "parentLastLoginAt" to null,
                    "parentIsOnline" to false,
                    "parentLastActiveMillis" to null,
                    "linkedParents" to emptyList<Any>(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            } else {
                mutableMapOf<String, Any?>(
                    "parentName" to parentName,
                    "parentEmail" to parentEmail,
                    "parentLinkedAt" to parentLinkedAt,
                    "parentLastLoginAt" to parentLastLoginAt,
                    "parentIsOnline" to true,
                    "parentLastActiveMillis" to nowMillis,
                    "linkedParents" to listOf(
                        mapOf(
                            "email" to (parentEmail ?: ""),
                            "name" to (parentName ?: ""),
                            "phone" to "",
                            "relationship" to "Veli",
                            "linkedAt" to (parentLinkedAt ?: ""),
                            "lastLoginAt" to parentLastLoginAt,
                            "isOnline" to true,
                            "lastActiveMillis" to nowMillis
                        )
                    ),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            }
            try {
                studentRef.update(updates).await()
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.UPDATE, studentRef.path)
            }
        }
        _students.value = _students.value.map { current ->
            if (current.id == studentId) {
                if (isDisconnecting) {
                    current.copy(
                        parentName = null,
                        parentEmail = null,
                        parentLinkedAt = null,
                        parentLastLoginAt = null,
                        parentIsOnline = false,
                        parentLastActiveMillis = null,
                        linkedParents = emptyList()
                    )
                } else {
                    val newAccount = LinkedParentAccount(
                        email = parentEmail ?: "veli@biriz.app",
                        name = parentName ?: "Öğrenci Velisi",
                        linkedAt = parentLinkedAt ?: "Bağlandı",
                        lastLoginAt = parentLastLoginAt,
                        isOnline = true,
                        lastActiveMillis = nowMillis
                    )
                    current.copy(
                        parentName = parentName,
                        parentEmail = parentEmail,
                        parentLinkedAt = parentLinkedAt,
                        parentLastLoginAt = parentLastLoginAt,
                        parentIsOnline = true,
                        parentLastActiveMillis = nowMillis,
                        linkedParents = listOf(newAccount)
                    )
                }
            } else current
        }
    }

    suspend fun unlinkParentFromStudent(
        classroomId: String?,
        studentId: String,
        parentEmail: String
    ) {
        if (isFirebaseAuthAvailable) {
            val disconnectPayload = mapOf<String, Any?>(
                "parentName" to null,
                "parentEmail" to null,
                "parentLinkedAt" to null,
                "parentLastLoginAt" to null,
                "parentIsOnline" to false,
                "parentLastActiveMillis" to null,
                "linkedParents" to emptyList<Any>(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            try {
                if (!classroomId.isNullOrBlank()) {
                    val studentRef = db.collection("classrooms").document(classroomId).collection("students").document(studentId)
                    studentRef.update(disconnectPayload).await()
                } else {
                    val classSnap = db.collection("classrooms").get().await()
                    for (cDoc in classSnap.documents) {
                        val studentRef = db.collection("classrooms").document(cDoc.id)
                            .collection("students").document(studentId)
                        val sDoc = studentRef.get().await()
                        if (sDoc.exists()) {
                            studentRef.update(disconnectPayload).await()
                            break
                        }
                    }
                }
            } catch (_: Exception) {}
        }
        _students.value = _students.value.map { current ->
            if (current.id == studentId) {
                current.copy(
                    parentName = null,
                    parentEmail = null,
                    parentLinkedAt = null,
                    parentLastLoginAt = null,
                    parentIsOnline = false,
                    parentLastActiveMillis = null,
                    linkedParents = emptyList()
                )
            } else current
        }
    }

    suspend fun updateParentPresence(
        classroomId: String?,
        studentIds: List<String>,
        parentEmail: String,
        parentName: String,
        isOnline: Boolean
    ) {
        if (isAccountDeleting || isGlobalAccountDeleting) return
        if (studentIds.isEmpty()) return
        val nowMillis = System.currentTimeMillis()
        val nowFormatted = java.text.SimpleDateFormat("dd MMMM yyyy, HH:mm", java.util.Locale("tr"))
            .format(java.util.Date(nowMillis))
        val cleanEmail = parentEmail.trim().ifBlank { "veli@biriz.app" }

        // 1. Update local _students StateFlow immediately (per-parent isolation)
        _students.value = _students.value.map { current ->
            if (current.id in studentIds && current.isParentLinked) {
                val updatedLinked = current.allLinkedParents.map { p ->
                    if (p.email.trim().equals(cleanEmail, ignoreCase = true) || current.allLinkedParents.size == 1) {
                        p.copy(
                            lastLoginAt = nowFormatted,
                            isOnline = isOnline,
                            lastActiveMillis = nowMillis
                        )
                    } else p
                }
                val anyOnline = updatedLinked.any { it.isCurrentlyActive(nowMillis) }
                current.copy(
                    parentLastLoginAt = nowFormatted,
                    parentIsOnline = anyOnline,
                    parentLastActiveMillis = nowMillis,
                    linkedParents = updatedLinked
                )
            } else current
        }

        // 2. Sync to Firestore in parallel across all linked students without blocking
        if (isFirebaseAuthAvailable && !isAccountDeleting && !isGlobalAccountDeleting && !classroomId.isNullOrBlank() && classroomId != "default") {
            val currentStudentsSnapshot = _students.value
            coroutineScope {
                studentIds.distinct().map { sId ->
                    async {
                        try {
                            val localStudent = currentStudentsSnapshot.find { it.id == sId }
                            if (localStudent != null && !localStudent.isParentLinked) {
                                return@async
                            }

                            val sRef = db.collection("classrooms").document(classroomId).collection("students").document(sId)
                            val effectiveLinkedParents = if (localStudent != null && localStudent.allLinkedParents.isNotEmpty()) {
                                localStudent.allLinkedParents
                            } else {
                                val remoteSnap = withTimeoutOrNull(2500L) { sRef.get().await() }
                                if (remoteSnap != null && remoteSnap.exists()) {
                                    parseLinkedParentsFromDoc(remoteSnap, includeLegacyFallback = true).map { p ->
                                        if (p.email.trim().equals(cleanEmail, ignoreCase = true)) {
                                            p.copy(
                                                lastLoginAt = nowFormatted,
                                                isOnline = isOnline,
                                                lastActiveMillis = nowMillis
                                            )
                                        } else p
                                    }
                                } else emptyList()
                            }

                            val anyOnline = if (effectiveLinkedParents.isNotEmpty()) {
                                effectiveLinkedParents.any { it.isCurrentlyActive(nowMillis) }
                            } else isOnline

                            val updates = mutableMapOf<String, Any?>(
                                "parentLastLoginAt" to nowFormatted,
                                "parentIsOnline" to anyOnline,
                                "parentLastActiveMillis" to nowMillis,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )

                            if (effectiveLinkedParents.isNotEmpty()) {
                                updates["linkedParents"] = effectiveLinkedParents.map { p ->
                                    mapOf(
                                        "email" to p.email,
                                        "name" to p.name,
                                        "phone" to p.phone,
                                        "relationship" to p.relationship,
                                        "linkedAt" to p.linkedAt,
                                        "lastLoginAt" to (p.lastLoginAt ?: nowFormatted),
                                        "isOnline" to p.isOnline,
                                        "lastActiveMillis" to (p.lastActiveMillis ?: nowMillis)
                                    )
                                }
                            }

                            withTimeoutOrNull(3500L) {
                                sRef.update(updates).await()
                            }
                        } catch (_: Exception) {}
                    }
                }.awaitAll()
            }
        }
    }

    suspend fun updateTeacherPresence(
        classroomId: String?,
        isOnline: Boolean
    ) {
        if (isAccountDeleting || isGlobalAccountDeleting) return
        val cid = classroomId?.trim().orEmpty()
        if (cid.isEmpty()) return
        val nowMillis = System.currentTimeMillis()

        // 1. Update local _registeredClasses immediately
        _registeredClasses.value = _registeredClasses.value.map { current ->
            if (current.id == cid) {
                current.copy(
                    teacherIsOnline = isOnline,
                    teacherLastActiveMillis = nowMillis
                )
            } else current
        }

        // 2. Sync to Firestore classrooms/{cid} so Parents see live teacher connectivity
        if (isFirebaseAuthAvailable && !isAccountDeleting && !isGlobalAccountDeleting && cid != "default") {
            try {
                db.collection("classrooms").document(cid)
                    .update(
                        mapOf(
                            "teacherIsOnline" to isOnline,
                            "teacherLastActiveMillis" to nowMillis,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
            } catch (_: Exception) {}
        }
    }

    fun toggleStudentPriority(studentId: String): Boolean {
        var isNowPriority = false
        _students.value = _students.value.map { current ->
            if (current.id == studentId) {
                val updated = !current.isPriority
                isNowPriority = updated
                current.copy(isPriority = updated)
            } else current
        }
        return isNowPriority
    }

    // --- Profil & Cascade Delete İşlemleri ---

    suspend fun deleteTeacherRoleAndClassroom(classroomId: String, context: Context? = null) {
        val uid = requireUserId()
        studentsJob?.cancel()
        studentsJob = null

        if (Firebase.auth.currentUser != null) {
            if (context != null) {
                try {
                    SmartBoardHelper.deleteUserSmartBoardTraces(
                        context = context,
                        ownedClassIds = setOf(classroomId),
                        teacherEmail = Firebase.auth.currentUser?.email.orEmpty(),
                        activeSessionCode = null
                    )
                } catch (_: Exception) {}
            }

            deleteOwnedClassroomCompletely(classroomId)

            val userRef = db.collection("users").document(uid)
            try {
                userRef.update(
                    mapOf(
                        "role" to "UYE",
                        "assignedClassId" to null,
                        "assignedSchoolId" to null,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.UPDATE, userRef.path)
            }
        }

        _registeredClasses.value = _registeredClasses.value.filter { it.id != classroomId }
        _students.value = emptyList()
    }

    suspend fun deleteParentRoleAndUnlinkAll(userEmail: String, uid: String) {
        if (isFirebaseAuthAvailable) {
            val allClassIds = try {
                withTimeoutOrNull(6000L) {
                    db.collection("classrooms").get().await().documents.map { it.id }
                } ?: _registeredClasses.value.map { it.id }
            } catch (_: Exception) {
                _registeredClasses.value.map { it.id }
            }
            if (allClassIds.isNotEmpty() && userEmail.isNotBlank()) {
                purgeParentAndUserTracesInOtherClassrooms(
                    otherClassroomIds = allClassIds,
                    userEmail = userEmail,
                    uid = uid
                )
            }
            if (uid.isNotBlank()) {
                updateUserRole(uid, "UYE")
            }
        }
    }

    fun markTeacherOfflineFireAndForget(classroomId: String?) {
        if (isAccountDeleting || isGlobalAccountDeleting) return
        val cid = classroomId?.trim().orEmpty()
        if (cid.isBlank() || cid == "default") return
        val nowMillis = System.currentTimeMillis()
        scope.launch {
            if (isAccountDeleting || isGlobalAccountDeleting) return@launch
            withContext(NonCancellable) {
                try {
                    withTimeoutOrNull(2500L) {
                        db.collection("classrooms").document(cid)
                            .update(
                                mapOf(
                                    "teacherIsOnline" to false,
                                    "teacherLastActiveMillis" to nowMillis,
                                    "updatedAt" to FieldValue.serverTimestamp()
                                )
                            ).await()
                    }
                } catch (_: Exception) {}
            }
        }
    }

    fun markParentOfflineFireAndForget(
        classroomId: String?,
        studentIds: List<String>,
        parentEmail: String
    ) {
        if (isAccountDeleting || isGlobalAccountDeleting) return
        val cid = classroomId?.trim().orEmpty()
        if (cid.isBlank() || cid == "default" || studentIds.isEmpty()) return
        val nowMillis = System.currentTimeMillis()
        val nowFormatted = java.text.SimpleDateFormat("dd MMMM yyyy, HH:mm", java.util.Locale("tr"))
            .format(java.util.Date(nowMillis))
        val cleanEmail = parentEmail.trim()
        scope.launch {
            withContext(NonCancellable) {
                for (sId in studentIds) {
                    try {
                        withTimeoutOrNull(2500L) {
                            val sRef = db.collection("classrooms").document(cid).collection("students").document(sId)
                            val snap = sRef.get().await()
                            if (!snap.exists()) return@withTimeoutOrNull
                            val existingParents = parseLinkedParentsFromDoc(snap, includeLegacyFallback = true)
                            if (existingParents.isEmpty()) return@withTimeoutOrNull
                            val updatedParents = existingParents.map { p ->
                                if (cleanEmail.isBlank() || p.email.trim().equals(cleanEmail, ignoreCase = true)) {
                                    p.copy(
                                        lastLoginAt = nowFormatted,
                                        isOnline = false,
                                        lastActiveMillis = nowMillis
                                    )
                                } else p
                            }
                            val anyOnline = updatedParents.any { it.isCurrentlyActive(nowMillis) }
                            sRef.update(
                                mapOf(
                                    "parentLastLoginAt" to nowFormatted,
                                    "parentIsOnline" to anyOnline,
                                    "parentLastActiveMillis" to nowMillis,
                                    "linkedParents" to updatedParents.map { p ->
                                        mapOf(
                                            "email" to p.email,
                                            "name" to p.name,
                                            "phone" to p.phone,
                                            "relationship" to p.relationship,
                                            "linkedAt" to p.linkedAt,
                                            "lastLoginAt" to (p.lastLoginAt ?: nowFormatted),
                                            "isOnline" to p.isOnline,
                                            "lastActiveMillis" to (p.lastActiveMillis ?: nowMillis)
                                        )
                                    },
                                    "updatedAt" to FieldValue.serverTimestamp()
                                )
                            ).await()
                        }
                    } catch (_: Exception) {}
                }
            }
        }
    }

    private suspend fun deleteAllDocsInCollection(collectionRef: CollectionReference) {
        try {
            var safetyPages = 0
            while (safetyPages < 25) {
                safetyPages++
                val snap = withTimeoutOrNull(9000L) {
                    collectionRef.limit(300).get().await()
                } ?: break
                if (snap.isEmpty) break

                val docs = snap.documents
                val batchCommitted = try {
                    withTimeoutOrNull(9000L) {
                        val batch = db.batch()
                        docs.forEach { doc -> batch.delete(doc.reference) }
                        batch.commit().await()
                        true
                    } ?: false
                } catch (_: Exception) {
                    false
                }

                if (!batchCommitted) {
                    // Fallback: tek tek sil
                    coroutineScope {
                        docs.chunked(25).forEach { miniChunk ->
                            miniChunk.map { doc ->
                                async {
                                    try {
                                        withTimeoutOrNull(3000L) { doc.reference.delete().await() }
                                    } catch (_: Exception) {}
                                }
                            }.awaitAll()
                        }
                    }
                }

                if (docs.size < 300) break
            }
        } catch (_: Exception) {}
    }

    private suspend fun deleteOwnedClassroomCompletely(classroomId: String) {
        if (classroomId.isBlank() || classroomId == "default") return
        val classRef = db.collection("classrooms").document(classroomId)
        val classesDocRef = db.collection("classes").document(classroomId)

        _registeredClasses.value = _registeredClasses.value.filterNot { it.id == classroomId }

        // 1. ÖNCELİK #1: Ana sınıf belgelerini (classrooms/{id} ve classes/{id}) İLK SANİYEDE sil!
        // Böylece alt koleksiyonlarda 1.500+ yıldız/kitap kaydı silinirken bile sınıf belgesi anında yok olur.
        try {
            withTimeoutOrNull(6000L) { classRef.delete().await() }
        } catch (_: Exception) {}
        try {
            withTimeoutOrNull(4000L) { classesDocRef.delete().await() }
        } catch (_: Exception) {}

        // 2. Kök tournaments/{tid} ve matches alt koleksiyonlarını paralel temizle
        try {
            val tournamentIds = mutableSetOf<String>()
            withTimeoutOrNull(5000L) {
                classRef.collection("tournaments").get().await().documents.forEach { tournamentIds.add(it.id) }
            }
            withTimeoutOrNull(5000L) {
                db.collection("tournaments").whereEqualTo("classId", classroomId).get().await().documents.forEach {
                    tournamentIds.add(it.id)
                }
            }
            if (tournamentIds.isNotEmpty()) {
                coroutineScope {
                    tournamentIds.map { tid ->
                        async {
                            deleteAllDocsInCollection(db.collection("tournaments").document(tid).collection("matches"))
                            try {
                                withTimeoutOrNull(4000L) { db.collection("tournaments").document(tid).delete().await() }
                            } catch (_: Exception) {}
                        }
                    }.awaitAll()
                }
            }
        } catch (_: Exception) {}

        // 3. Sınıfın tüm 14 alt koleksiyonunu PARALEL ve sayfalı (paginated) olarak temizle
        val subcollections = listOf(
            "students",
            "announcements",
            "messages",
            "chat_clear_state",
            "groups",
            "library_books",
            "reading_records",
            "lucky_student",
            "schedule",
            "seating",
            "stars",
            "star_records",
            "tournaments",
            "live_board"
        )
        coroutineScope {
            val subJobs = subcollections.map { sub ->
                async {
                    deleteAllDocsInCollection(classRef.collection(sub))
                }
            } + async {
                deleteAllDocsInCollection(classesDocRef.collection("live_board"))
            }
            subJobs.awaitAll()
        }

        // 4. Alt koleksiyon temizliği sırasında gecikmeli bir yazma ihtimaline karşı kök belgeleri son kez sil
        try {
            withTimeoutOrNull(5000L) { classRef.delete().await() }
        } catch (_: Exception) {}
        try {
            withTimeoutOrNull(4000L) { classesDocRef.delete().await() }
        } catch (_: Exception) {}
    }

    private suspend fun purgeParentAndUserTracesInOtherClassrooms(
        otherClassroomIds: List<String>,
        userEmail: String,
        uid: String
    ) {
        val cleanEmail = userEmail.trim()
        val parentClearKey = if (cleanEmail.isNotBlank()) {
            ChatVisibilityHelper.viewerDocumentKey(viewerIsTeacher = false, userEmail = cleanEmail)
        } else ""

        coroutineScope {
            otherClassroomIds.map { cid ->
                async {
                    withTimeoutOrNull(8000L) {
                        val classRef = db.collection("classrooms").document(cid)

                        // 1. Öğrencilerden veli e-postasını/profilini temizle (diğer veliler varsa koru)
                        if (cleanEmail.isNotBlank()) {
                            try {
                                val studentSnaps = classRef.collection("students").get().await()
                                for (sDoc in studentSnaps.documents) {
                                    val sParentEmail = sDoc.getString("parentEmail")?.trim().orEmpty()
                                    val existingParents = parseLinkedParentsFromDoc(sDoc, includeLegacyFallback = true)
                                    val hasEmail = sParentEmail.equals(cleanEmail, ignoreCase = true) ||
                                        existingParents.any { it.email.trim().equals(cleanEmail, ignoreCase = true) }

                                    if (hasEmail) {
                                        val remainingParents = existingParents.filterNot {
                                            it.email.trim().equals(cleanEmail, ignoreCase = true)
                                        }
                                        if (remainingParents.isEmpty()) {
                                            sDoc.reference.update(
                                                mapOf<String, Any?>(
                                                    "parentName" to null,
                                                    "parentEmail" to null,
                                                    "parentLinkedAt" to null,
                                                    "parentLastLoginAt" to null,
                                                    "parentIsOnline" to false,
                                                    "parentLastActiveMillis" to null,
                                                    "linkedParents" to emptyList<Any>(),
                                                    "updatedAt" to FieldValue.serverTimestamp()
                                                )
                                            ).await()
                                        } else {
                                            val primary = remainingParents.first()
                                            sDoc.reference.update(
                                                mapOf<String, Any?>(
                                                    "parentName" to primary.name,
                                                    "parentEmail" to primary.email,
                                                    "parentLinkedAt" to primary.linkedAt,
                                                    "parentLastLoginAt" to primary.lastLoginAt,
                                                    "parentIsOnline" to remainingParents.any { it.isOnline },
                                                    "parentLastActiveMillis" to primary.lastActiveMillis,
                                                    "linkedParents" to remainingParents.map { p ->
                                                        mapOf(
                                                            "email" to p.email,
                                                            "name" to p.name,
                                                            "phone" to p.phone,
                                                            "relationship" to p.relationship,
                                                            "linkedAt" to p.linkedAt,
                                                            "lastLoginAt" to p.lastLoginAt,
                                                            "isOnline" to p.isOnline,
                                                            "lastActiveMillis" to p.lastActiveMillis
                                                        )
                                                    },
                                                    "updatedAt" to FieldValue.serverTimestamp()
                                                )
                                            ).await()
                                        }
                                    }
                                }
                            } catch (_: Exception) {}

                            // 2. Bu velinin gönderdiği bireysel sohbet mesajlarını temizle
                            try {
                                val msgSnaps = classRef.collection("messages").get().await()
                                val toDelete = msgSnaps.documents.filter { d ->
                                    val isFromTeacher = d.getBoolean("isFromTeacher") ?: false
                                    val msgParentEmail = d.getString("parentEmail")?.trim().orEmpty()
                                    !isFromTeacher && msgParentEmail.equals(cleanEmail, ignoreCase = true)
                                }
                                toDelete.chunked(300).forEach { chunk ->
                                    val batch = db.batch()
                                    chunk.forEach { batch.delete(it.reference) }
                                    batch.commit().await()
                                }
                            } catch (_: Exception) {}
                        }

                        // 3. Kullanıcının chat_clear_state kayıtlarını temizle
                        val clearDocKeys = listOfNotNull(
                            parentClearKey.takeIf { it.isNotBlank() },
                            uid.takeIf { it.isNotBlank() }
                        ).distinct()
                        for (key in clearDocKeys) {
                            try {
                                classRef.collection("chat_clear_state").document(key).delete().await()
                            } catch (_: Exception) {}
                        }
                    }
                }
            }.awaitAll()
        }
    }

    private fun deletedAccountTombstoneId(email: String, uid: String): String {
        val normEmail = email.trim().lowercase(java.util.Locale.ROOT)
            .replace(Regex("[^a-z0-9._-]"), "_")
        return if (normEmail.isNotBlank()) "email_$normEmail" else "uid_$uid"
    }

    private suspend fun deleteFirestoreDocDirectRest(relativeDocPath: String, bearerToken: String? = null) = withContext(Dispatchers.IO) {
        if (isTestMode) return@withContext
        val cleanPath = relativeDocPath.trim('/')
        if (cleanPath.isBlank()) return@withContext
        try {
            val url = "https://firestore.googleapis.com/v1/projects/$DIRECT_REST_PROJECT_ID/databases/$DIRECT_REST_DB_ID/documents/$cleanPath"
            val reqBuilder = okhttp3.Request.Builder()
                .url(url)
                .delete()
            val tokenToUse = bearerToken ?: try {
                withTimeoutOrNull(1800L) {
                    Firebase.auth.currentUser?.getIdToken(false)?.await()?.token
                }
            } catch (_: Exception) {
                null
            }
            if (!tokenToUse.isNullOrBlank()) {
                reqBuilder.header("Authorization", "Bearer $tokenToUse")
            }
            directRestHttpClient.newCall(reqBuilder.build()).execute().use { _ -> }
        } catch (_: Exception) {}
    }

    private suspend fun deleteUserDocumentEverywhere(userDocId: String, bearerToken: String? = null) {
        val cleanId = userDocId.trim()
        if (cleanId.isBlank()) return
        coroutineScope {
            val sdkJob = async {
                try {
                    withTimeoutOrNull(5000L) {
                        db.collection("users").document(cleanId).delete().await()
                    }
                } catch (_: Exception) {}
            }
            val restJob = async {
                deleteFirestoreDocDirectRest("users/$cleanId", bearerToken)
            }
            sdkJob.await()
            restJob.await()
        }
    }

    suspend fun clearAccountDeletionTombstone(uid: String, email: String) {
        if (!isFirebaseAuthAvailable) return
        val tombstoneId = deletedAccountTombstoneId(email, uid)
        try {
            withTimeoutOrNull(3000L) {
                db.collection("deleted_accounts").document(tombstoneId).delete().await()
            }
        } catch (_: Exception) {}
        deleteFirestoreDocDirectRest("deleted_accounts/$tombstoneId")
    }

    /**
     * Firebase Authentication'dan silinmiş olduğu halde Firestore `users` koleksiyonunda
     * kalmış olan geçersiz/yetim (orphaned) kullanıcı belgelerini ve aynı e-postaya ait
     * eski UID kayıtlarını otomatik olarak tespit edip kalıcı olarak siler.
     */
    suspend fun purgeStaleAndOrphanedUserDocuments(activeUid: String, activeEmail: String) {
        if (isAccountDeleting || isGlobalAccountDeleting || !isFirebaseAuthAvailable) return
        val cleanActiveEmail = activeEmail.trim().lowercase(java.util.Locale.ROOT)
        val bearerToken = try {
            withTimeoutOrNull(2000L) {
                Firebase.auth.currentUser?.getIdToken(false)?.await()?.token
            }
        } catch (_: Exception) {
            null
        }

        val orphanUserIdsToDelete = linkedSetOf<String>()
        val orphanEmailsToUnlink = linkedSetOf<String>()
        val orphanClassIdsToDelete = linkedSetOf<String>()

        // 1. Önce bilinen geçersiz/silinmiş UID kayıtlarını hedef listesine ekle (aktif kullanıcı hariç)
        KNOWN_ORPHANED_USER_DOC_IDS.forEach { orphanId ->
            if (orphanId != activeUid) {
                orphanUserIdsToDelete.add(orphanId)
            }
        }

        // 2. Varsa deleted_accounts mezar taşlarını oku
        val tombstoneDocs = try {
            withTimeoutOrNull(3500L) {
                db.collection("deleted_accounts").get(Source.SERVER).await().documents
            } ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
        val tombstonedEmails = linkedSetOf<String>()
        for (tDoc in tombstoneDocs) {
            tDoc.getString("uid")?.trim()?.takeIf { it.isNotBlank() && it != activeUid }?.let {
                orphanUserIdsToDelete.add(it)
            }
            tDoc.getString("email")?.trim()?.lowercase(java.util.Locale.ROOT)?.takeIf {
                it.isNotBlank() && it != cleanActiveEmail
            }?.let {
                tombstonedEmails.add(it)
                orphanEmailsToUnlink.add(it)
            }
            @Suppress("UNCHECKED_CAST")
            (tDoc.get("deletedUserDocIds") as? List<String>)?.forEach { id ->
                if (id.isNotBlank() && id != activeUid) orphanUserIdsToDelete.add(id)
            }
            @Suppress("UNCHECKED_CAST")
            (tDoc.get("deletedClassIds") as? List<String>)?.forEach { cid ->
                if (cid.isNotBlank() && cid != "default") orphanClassIdsToDelete.add(cid)
            }
        }

        // 3. Firestore `users` koleksiyonunu sunucudan tara
        try {
            val usersSnap = withTimeoutOrNull(5000L) {
                db.collection("users").get(Source.SERVER).await()
            } ?: withTimeoutOrNull(3500L) {
                db.collection("users").get().await()
            }
            usersSnap?.documents?.forEach { uDoc ->
                val docId = uDoc.id
                if (docId == activeUid) return@forEach

                val docEmail = uDoc.getString("email")?.trim()?.lowercase(java.util.Locale.ROOT).orEmpty()
                val docUserId = uDoc.getString("userId")?.trim().orEmpty()
                val docAssignedClassId = uDoc.getString("assignedClassId")?.trim().orEmpty()

                val isKnownOrphan = docId in KNOWN_ORPHANED_USER_DOC_IDS
                val isDuplicateOldUidForActiveUser = cleanActiveEmail.isNotBlank() && docEmail == cleanActiveEmail
                val isSameUidMismatch = docUserId == activeUid
                val isTombstoned = docId in orphanUserIdsToDelete || (docEmail.isNotBlank() && docEmail in tombstonedEmails)
                val isGhostIncompleteDoc = docEmail.isBlank() && docUserId.isBlank()

                if (isKnownOrphan || isDuplicateOldUidForActiveUser || isSameUidMismatch || isTombstoned || isGhostIncompleteDoc) {
                    orphanUserIdsToDelete.add(docId)
                    if (docEmail.isNotBlank() && docEmail != cleanActiveEmail) {
                        orphanEmailsToUnlink.add(docEmail)
                    }
                    if (docAssignedClassId.isNotBlank() && docAssignedClassId != "default" && !isDuplicateOldUidForActiveUser) {
                        orphanClassIdsToDelete.add(docAssignedClassId)
                    }
                }
            }
        } catch (_: Exception) {}

        // 4. Tespit edilen tüm geçersiz `users` belgelerini hem SDK hem REST üzerinden paralel sil
        if (orphanUserIdsToDelete.isNotEmpty()) {
            coroutineScope {
                orphanUserIdsToDelete.map { orphanId ->
                    async {
                        deleteUserDocumentEverywhere(orphanId, bearerToken)
                    }
                }.awaitAll()
            }
        }

        // 5. Silinmiş veli hesaplarının öğrenciler üzerindeki eski bağlantılarını kaldır
        if (orphanEmailsToUnlink.isNotEmpty()) {
            try {
                val allClassIds = withTimeoutOrNull(4000L) {
                    db.collection("classrooms").get().await().documents.map { it.id }
                } ?: emptyList()
                for (orphanEmail in orphanEmailsToUnlink) {
                    purgeParentAndUserTracesInOtherClassrooms(
                        otherClassroomIds = allClassIds,
                        userEmail = orphanEmail,
                        uid = ""
                    )
                }
            } catch (_: Exception) {}
        }

        // 6. Varsa mezar taşlarını da temizle
        for (tDoc in tombstoneDocs) {
            val tEmail = tDoc.getString("email")?.trim()?.lowercase(java.util.Locale.ROOT).orEmpty()
            if (tEmail != cleanActiveEmail) {
                try {
                    withTimeoutOrNull(2500L) { tDoc.reference.delete().await() }
                } catch (_: Exception) {}
                deleteFirestoreDocDirectRest("deleted_accounts/${tDoc.id}", bearerToken)
            }
        }
    }

    suspend fun deleteEntireAccount(
        context: Context? = null,
        currentRole: UserRole = UserRole.MEMBER,
        activeClassId: String? = null,
        activeBoardSessionCode: String? = null
    ) {
        val fbUser = Firebase.auth.currentUser
        val uid = fbUser?.uid ?: if (isTestMode) (testUserId ?: "test_uid") else requireUserId()
        val userEmail = fbUser?.email?.trim().orEmpty()
        val cleanEmail = userEmail.lowercase(java.util.Locale.ROOT)

        // 1. Tüm canlı Firestore dinleyicilerini ve arka plan yazmalarını İLK MİLİSANİYEDE durdur!
        // Böylece reauthenticate veya silme sırasında hiçbir dinleyici users/{uid} belgesini tekrar oluşturamaz.
        stopAllListenersForAccountDeletion()

        try {
            // 2. Oturum süresi eskidiyse (4 dk+) veritabanını silmeden önce güvenli Google doğrulaması iste.
            if (fbUser != null && context != null) {
                val lastSignIn = fbUser.metadata?.lastSignInTimestamp ?: 0L
                if (lastSignIn > 0L && (System.currentTimeMillis() - lastSignIn) > 4L * 60L * 1000L) {
                    val reauthed = GoogleAuthHelper.reauthenticateCurrentUser(context)
                    if (!reauthed) {
                        throw IllegalStateException("Güvenlik doğrulaması iptal edildiği için hesabınız silinmedi.")
                    }
                }
            }

            // Doğrulama sonrası güncel Bearer token'ı al (REST silme çağrıları için)
            val bearerToken = try {
                withTimeoutOrNull(2500L) {
                    fbUser?.getIdToken(false)?.await()?.token
                }
            } catch (_: Exception) {
                null
            }

            if (fbUser != null) {
                // 3. FCM tokenını en başta iptal et ki silme bittikten sonra onNewToken tetiklenmesin
                try {
                    withTimeoutOrNull(2500L) { FirebaseMessaging.getInstance().deleteToken().await() }
                } catch (_: Exception) {}

                // 4. Bu kullanıcıya veya e-postasına ait TÜM users belgelerini ve atanmış sınıfları tespit et
                val matchedUserDocIds = linkedSetOf<String>()
                val assignedClassIdsFromUsers = linkedSetOf<String>()
                if (uid.isNotBlank()) matchedUserDocIds.add(uid)
                matchedUserDocIds.addAll(KNOWN_ORPHANED_USER_DOC_IDS)

                try {
                    val directUserDoc = withTimeoutOrNull(4500L) {
                        db.collection("users").document(uid).get(Source.SERVER).await()
                    } ?: withTimeoutOrNull(3000L) {
                        db.collection("users").document(uid).get().await()
                    }
                    if (directUserDoc != null && directUserDoc.exists()) {
                        directUserDoc.getString("assignedClassId")?.trim()?.takeIf { it.isNotBlank() && it != "default" }?.let {
                            assignedClassIdsFromUsers.add(it)
                        }
                    }
                } catch (_: Exception) {}

                try {
                    val allUsersSnap = withTimeoutOrNull(5000L) {
                        db.collection("users").get(Source.SERVER).await()
                    } ?: withTimeoutOrNull(3500L) {
                        db.collection("users").get().await()
                    }
                    allUsersSnap?.documents?.forEach { uDoc ->
                        val docEmail = uDoc.getString("email")?.trim().orEmpty()
                        val docUserId = uDoc.getString("userId")?.trim().orEmpty()
                        if (uDoc.id == uid ||
                            docUserId == uid ||
                            (cleanEmail.isNotBlank() && docEmail.equals(cleanEmail, ignoreCase = true))
                        ) {
                            matchedUserDocIds.add(uDoc.id)
                            uDoc.getString("assignedClassId")?.trim()?.takeIf { it.isNotBlank() && it != "default" }?.let {
                                assignedClassIdsFromUsers.add(it)
                            }
                        }
                    }
                } catch (_: Exception) {}

                // 5. Sahip olunan sınıfları ve paylaşılan sınıfları ayır (Veli asla öğretmenin sınıfını silemez!)
                val allClassDocs = try {
                    withTimeoutOrNull(6000L) {
                        db.collection("classrooms").get(Source.SERVER).await().documents
                    } ?: withTimeoutOrNull(4000L) {
                        db.collection("classrooms").get().await().documents
                    } ?: emptyList()
                } catch (_: Exception) {
                    emptyList()
                }

                val ownedClassIds = linkedSetOf<String>()
                val otherClassIds = linkedSetOf<String>()

                ownedClassIds.addAll(assignedClassIdsFromUsers)

                for (cDoc in allClassDocs) {
                    val docTeacherId = cDoc.getString("teacherId")?.trim().orEmpty()
                    val docTeacherEmail = cDoc.getString("teacherEmail")?.trim().orEmpty()
                    val isOwnerMatch = (docTeacherId.isNotBlank() && docTeacherId in matchedUserDocIds) ||
                        (cleanEmail.isNotBlank() && docTeacherEmail.equals(cleanEmail, ignoreCase = true)) ||
                        (cDoc.id in assignedClassIdsFromUsers)
                    val isFallbackActiveTeacherClass = currentRole == UserRole.TEACHER &&
                        !activeClassId.isNullOrBlank() &&
                        activeClassId != "default" &&
                        cDoc.id == activeClassId &&
                        (docTeacherId.isBlank() || docTeacherId in matchedUserDocIds || docTeacherEmail.equals(cleanEmail, ignoreCase = true))

                    if (isOwnerMatch || isFallbackActiveTeacherClass) {
                        ownedClassIds.add(cDoc.id)
                    } else {
                        otherClassIds.add(cDoc.id)
                    }
                }

                if (currentRole == UserRole.TEACHER &&
                    !activeClassId.isNullOrBlank() &&
                    activeClassId != "default" &&
                    activeClassId !in otherClassIds
                ) {
                    ownedClassIds.add(activeClassId)
                }

                if (com.example.util.WebDataMigrationHelper.isTargetTeacherEmail(cleanEmail) &&
                    defaultSampleClass.id !in otherClassIds
                ) {
                    ownedClassIds.add(defaultSampleClass.id)
                }

                // 6. KÖK (users + classrooms) belgelerini İLK ADIMDA (kota harcamadan doğrudan) hem SDK hem REST ile sil!
                val tombstoneId = deletedAccountTombstoneId(cleanEmail, uid)

                coroutineScope {
                    matchedUserDocIds.map { uId ->
                        async { deleteUserDocumentEverywhere(uId, bearerToken) }
                    }.awaitAll()
                    ownedClassIds.map { cId ->
                        async {
                            try {
                                withTimeoutOrNull(4000L) { db.collection("classrooms").document(cId).delete().await() }
                            } catch (_: Exception) {}
                            deleteFirestoreDocDirectRest("classrooms/$cId", bearerToken)
                            try {
                                withTimeoutOrNull(3000L) { db.collection("classes").document(cId).delete().await() }
                            } catch (_: Exception) {}
                            deleteFirestoreDocDirectRest("classes/$cId", bearerToken)
                        }
                    }.awaitAll()
                }

                // 7. Akıllı Tahta oturumlarını ve chunk verilerini sil
                if (context != null) {
                    try {
                        SmartBoardHelper.deleteUserSmartBoardTraces(
                            context = context,
                            ownedClassIds = ownedClassIds,
                            teacherEmail = userEmail,
                            activeSessionCode = activeBoardSessionCode
                        )
                    } catch (e: Exception) {
                        Log.w("ClassRegistryRepo", "Failed clearing SmartBoard traces", e)
                    }
                }

                // 8. Öğretmene ait tüm sınıfları ve 14 alt koleksiyonunu paralel sil
                coroutineScope {
                    ownedClassIds.map { ownedId ->
                        async {
                            try {
                                deleteOwnedClassroomCompletely(ownedId)
                            } catch (e: Exception) {
                                Log.w("ClassRegistryRepo", "Failed deleting owned classroom $ownedId", e)
                            }
                        }
                    }.awaitAll()
                }

                // 9. Diğer sınıflardaki veli/mesaj/temizleme izlerini temizle
                purgeParentAndUserTracesInOtherClassrooms(
                    otherClassroomIds = otherClassIds.toList(),
                    userEmail = userEmail,
                    uid = uid
                )

                // 10. Kullanıcının oluşturduğu okul kayıtlarını temizle
                try {
                    withTimeoutOrNull(4500L) {
                        val schoolsSnap = db.collection("schools").whereEqualTo("creatorId", uid).get().await()
                        for (sDoc in schoolsSnap.documents) {
                            try {
                                sDoc.reference.delete().await()
                            } catch (_: Exception) {}
                            deleteFirestoreDocDirectRest("schools/${sDoc.id}", bearerToken)
                        }
                    }
                } catch (_: Exception) {}

                // 11. Kök users/{id} ve classrooms/{id} belgelerinin silindiğini hem SDK hem REST ile garanti altına al ve kuyruğu boşalt
                coroutineScope {
                    matchedUserDocIds.map { uId ->
                        async { deleteUserDocumentEverywhere(uId, bearerToken) }
                    }.awaitAll()
                    ownedClassIds.map { cId ->
                        async {
                            try {
                                withTimeoutOrNull(4000L) { db.collection("classrooms").document(cId).delete().await() }
                            } catch (_: Exception) {}
                            deleteFirestoreDocDirectRest("classrooms/$cId", bearerToken)
                        }
                    }.awaitAll()
                }
                try {
                    withTimeoutOrNull(3500L) { db.waitForPendingWrites().await() }
                } catch (_: Exception) {}

                // 12. Firebase Auth kullanıcı kaydını kalıcı olarak sil
                try {
                    fbUser.delete().await()
                } catch (e: FirebaseAuthRecentLoginRequiredException) {
                    if (context != null) {
                        val reauthed = GoogleAuthHelper.reauthenticateCurrentUser(context)
                        if (!reauthed) {
                            throw IllegalStateException("Verileriniz temizlendi; ancak kimlik kaydının silinmesi için Google doğrulaması gerekiyor.")
                        }
                        fbUser.delete().await()
                    } else {
                        throw e
                    }
                }

                // 13. Auth silindikten SONRA da REST üzerinden son kez users/{id} ve mezar taşı temizliği yap
                coroutineScope {
                    matchedUserDocIds.map { uId ->
                        async { deleteFirestoreDocDirectRest("users/$uId", bearerToken) }
                    }.awaitAll()
                    ownedClassIds.map { cId ->
                        async { deleteFirestoreDocDirectRest("classrooms/$cId", bearerToken) }
                    }.awaitAll()
                }
                try {
                    withTimeoutOrNull(2500L) { db.collection("deleted_accounts").document(tombstoneId).delete().await() }
                } catch (_: Exception) {}
                deleteFirestoreDocDirectRest("deleted_accounts/$tombstoneId", bearerToken)
            }

            // 14. Cihazdaki tüm yerel BİRİZ dosyalarını, bildirimleri, WorkManager işlerini ve önbelleği sil
            if (context != null) {
                AppDataWipeHelper.wipeAllLocalAppData(context)
            }

            clearSessionCaches()
        } finally {
            isAccountDeleting = false
            isGlobalAccountDeleting = false
        }
    }

    /**
     * Kullanıcı hesabını sildikten sonra aynı Google hesabıyla tekrar üye olduğunda
     * (yeni bir Firebase Auth UID aldığında veya deleted_accounts mezar taşı bulunduğunda),
     * önceki silinmiş hesaptan kalmış olabilecek tüm yetim (orphaned) Firestore verilerini
     * otomatik olarak temizler. Böylece kullanıcı %100 sıfır ve temiz bir hesapla başlar.
     */
    private suspend fun purgeOrphanedDataOnFreshRegistration(
        newUid: String,
        email: String,
        tombstoneDoc: com.google.firebase.firestore.DocumentSnapshot?
    ) {
        val cleanEmail = email.trim().lowercase(java.util.Locale.ROOT)
        if (cleanEmail.isBlank() && tombstoneDoc == null) return

        val hasTombstone = tombstoneDoc != null && tombstoneDoc.exists()
        val oldUids = linkedSetOf<String>()
        val tombstoneClassIds = linkedSetOf<String>()

        if (hasTombstone && tombstoneDoc != null) {
            tombstoneDoc.getString("uid")?.trim()?.takeIf { it.isNotBlank() }?.let { oldUids.add(it) }
            @Suppress("UNCHECKED_CAST")
            (tombstoneDoc.get("deletedUserDocIds") as? List<String>)?.forEach {
                if (it.isNotBlank()) oldUids.add(it)
            }
            @Suppress("UNCHECKED_CAST")
            (tombstoneDoc.get("deletedClassIds") as? List<String>)?.forEach {
                if (it.isNotBlank() && it != "default") tombstoneClassIds.add(it)
            }
        }

        // 1. E-postası eşleşen eski users belgelerini ve bilinen yetim kayıtları bul ve sil
        try {
            val usersSnap = withTimeoutOrNull(4500L) {
                db.collection("users").get(Source.SERVER).await()
            } ?: withTimeoutOrNull(3000L) {
                db.collection("users").get().await()
            }
            usersSnap?.documents?.forEach { uDoc ->
                val docEmail = uDoc.getString("email")?.trim().orEmpty()
                val isSameEmail = cleanEmail.isNotBlank() && docEmail.equals(cleanEmail, ignoreCase = true) && (uDoc.id != newUid || hasTombstone)
                val isKnownOrphan = uDoc.id in KNOWN_ORPHANED_USER_DOC_IDS && uDoc.id != newUid
                if (isSameEmail || isKnownOrphan) {
                    oldUids.add(uDoc.id)
                    if (isSameEmail) {
                        uDoc.getString("assignedClassId")?.trim()?.takeIf { it.isNotBlank() && it != "default" }?.let {
                            tombstoneClassIds.add(it)
                        }
                    }
                    deleteUserDocumentEverywhere(uDoc.id)
                }
            }
        } catch (_: Exception) {}

        // 2. E-postası veya eski UID'si eşleşen yetim sınıfları ve alt koleksiyonlarını tamamen sil
        try {
            val allClassDocs = withTimeoutOrNull(5000L) {
                db.collection("classrooms").get(Source.SERVER).await().documents
            } ?: withTimeoutOrNull(3500L) {
                db.collection("classrooms").get().await().documents
            } ?: emptyList()

            val orphanOwnedClassIds = linkedSetOf<String>()
            val otherClassIds = linkedSetOf<String>()
            orphanOwnedClassIds.addAll(tombstoneClassIds)

            for (cDoc in allClassDocs) {
                val docTeacherId = cDoc.getString("teacherId")?.trim().orEmpty()
                val docTeacherEmail = cDoc.getString("teacherEmail")?.trim().orEmpty()
                val matchesEmail = cleanEmail.isNotBlank() && docTeacherEmail.equals(cleanEmail, ignoreCase = true)
                val matchesOldUid = docTeacherId.isNotBlank() && docTeacherId in oldUids
                val isDifferentUidForSameEmail = matchesEmail && (docTeacherId != newUid || hasTombstone)

                if (matchesOldUid || isDifferentUidForSameEmail || cDoc.id in tombstoneClassIds) {
                    orphanOwnedClassIds.add(cDoc.id)
                } else {
                    otherClassIds.add(cDoc.id)
                }
            }

            if (orphanOwnedClassIds.isNotEmpty()) {
                coroutineScope {
                    orphanOwnedClassIds.map { cid ->
                        async {
                            try {
                                deleteOwnedClassroomCompletely(cid)
                            } catch (_: Exception) {}
                        }
                    }.awaitAll()
                }
            }

            // 3. Diğer sınıflardaki eski veli bağlantılarını temizle
            if (otherClassIds.isNotEmpty() && cleanEmail.isNotBlank()) {
                purgeParentAndUserTracesInOtherClassrooms(
                    otherClassroomIds = otherClassIds.toList(),
                    userEmail = cleanEmail,
                    uid = newUid
                )
            }
        } catch (_: Exception) {}

        // 4. Mezar taşını temizle
        if (hasTombstone && tombstoneDoc != null) {
            try {
                withTimeoutOrNull(3000L) { tombstoneDoc.reference.delete().await() }
            } catch (_: Exception) {}
            deleteFirestoreDocDirectRest("deleted_accounts/${tombstoneDoc.id}")
        }
    }

    suspend fun ensureUserProfile(uid: String, fullName: String, email: String): com.example.data.model.UserProfile {
        if (isAccountDeleting || isGlobalAccountDeleting || Firebase.auth.currentUser == null) {
            return com.example.data.model.UserProfile(
                id = uid,
                userId = uid,
                fullName = fullName,
                email = email,
                role = "UYE",
                kvkkAccepted = false,
                isFreshRegistration = true
            )
        }

        val cleanEmail = email.trim().lowercase(java.util.Locale.ROOT)
        val tombstoneId = deletedAccountTombstoneId(cleanEmail, uid)
        val userRef = db.collection("users").document(uid)

        val (snapshot, tombstoneSnap) = coroutineScope {
            val userDeferred = async {
                try {
                    withTimeoutOrNull(5000L) { userRef.get(Source.SERVER).await() }
                        ?: withTimeoutOrNull(3000L) { userRef.get().await() }
                } catch (_: Exception) {
                    try {
                        withTimeoutOrNull(2500L) { userRef.get().await() }
                    } catch (_: Exception) {
                        null
                    }
                }
            }
            val tombstoneDeferred = async {
                try {
                    withTimeoutOrNull(4000L) {
                        db.collection("deleted_accounts").document(tombstoneId).get(Source.SERVER).await()
                    } ?: withTimeoutOrNull(2000L) {
                        db.collection("deleted_accounts").document(tombstoneId).get().await()
                    }
                } catch (_: Exception) {
                    null
                }
            }
            Pair(userDeferred.await(), tombstoneDeferred.await())
        }

        if (isAccountDeleting || isGlobalAccountDeleting || Firebase.auth.currentUser?.uid != uid) {
            return com.example.data.model.UserProfile(
                id = uid,
                userId = uid,
                fullName = fullName,
                email = email,
                role = "UYE",
                kvkkAccepted = false,
                isFreshRegistration = true
            )
        }

        val wasAccountExplicitlyDeleted = tombstoneSnap != null && tombstoneSnap.exists()
        val isBrandNewUserOnServer = snapshot != null && !snapshot.exists()

        // Eğer kullanıcı hesabını silip tekrar üye olduysa (yeni UID veya silme mezar taşı varsa),
        // önceki hesaptan kalan tüm yetim Firestore verilerini temizle ve sıfırdan başlat!
        if (wasAccountExplicitlyDeleted || isBrandNewUserOnServer) {
            purgeOrphanedDataOnFreshRegistration(
                newUid = uid,
                email = cleanEmail,
                tombstoneDoc = tombstoneSnap
            )
            scope.launch {
                purgeStaleAndOrphanedUserDocuments(activeUid = uid, activeEmail = cleanEmail)
            }

            if (isAccountDeleting || isGlobalAccountDeleting || Firebase.auth.currentUser?.uid != uid) {
                return com.example.data.model.UserProfile(
                    id = uid,
                    userId = uid,
                    fullName = fullName,
                    email = email,
                    role = "UYE",
                    kvkkAccepted = false,
                    isFreshRegistration = true
                )
            }

            val freshPayload = mapOf(
                "userId" to uid,
                "fullName" to fullName,
                "email" to email,
                "role" to "UYE",
                "kvkkAccepted" to false,
                "assignedClassId" to null,
                "assignedSchoolId" to null,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            try {
                withTimeoutOrNull(5000L) {
                    userRef.set(freshPayload).await()
                }
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.CREATE, userRef.path)
            }

            if (Firebase.auth.currentUser != null && !isAccountDeleting && !isGlobalAccountDeleting) {
                startObserving()
            }

            return com.example.data.model.UserProfile(
                id = uid,
                userId = uid,
                fullName = fullName,
                email = email,
                role = "UYE",
                kvkkAccepted = false,
                assignedClassId = null,
                assignedSchoolId = null,
                isFreshRegistration = true
            )
        }

        // Mevcut kullanıcı girişinde de arka planda geçersiz/silinmiş diğer users belgelerini temizle
        scope.launch {
            purgeStaleAndOrphanedUserDocuments(activeUid = uid, activeEmail = cleanEmail)
        }

        if (snapshot != null && snapshot.exists()) {
            if (Firebase.auth.currentUser != null && !isAccountDeleting && !isGlobalAccountDeleting) {
                startObserving()
            }
            return com.example.data.model.UserProfile(
                id = snapshot.id,
                userId = snapshot.getString("userId") ?: uid,
                fullName = snapshot.getString("fullName") ?: fullName,
                email = snapshot.getString("email") ?: email,
                role = snapshot.getString("role") ?: "UYE",
                kvkkAccepted = snapshot.getBoolean("kvkkAccepted") ?: false,
                assignedClassId = snapshot.getString("assignedClassId"),
                assignedSchoolId = snapshot.getString("assignedSchoolId"),
                isFreshRegistration = false
            )
        }

        return com.example.data.model.UserProfile(
            id = uid,
            userId = uid,
            fullName = fullName,
            email = email,
            role = "UYE",
            kvkkAccepted = false,
            isFreshRegistration = false
        )
    }

    suspend fun fetchClassroomById(classroomId: String): ClassRoom? {
        if (classroomId.isBlank() || classroomId == "default") return null
        if (Firebase.auth.currentUser == null) {
            return _registeredClasses.value.firstOrNull { it.id == classroomId }
        }
        return try {
            val doc = withTimeoutOrNull(5000L) {
                db.collection("classrooms").document(classroomId).get(Source.SERVER).await()
            } ?: withTimeoutOrNull(3000L) {
                db.collection("classrooms").document(classroomId).get().await()
            }
            if (doc != null && doc.exists()) {
                val cityName = doc.getString("cityName") ?: ""
                val districtName = doc.getString("districtName") ?: ""
                val schoolName = doc.getString("schoolName") ?: ""
                val gradeLevel = (doc.getLong("gradeLevel") ?: 1L).toInt()
                val section = doc.getString("section") ?: "A"
                val teacherId = doc.getString("teacherId") ?: ""
                val teacherName = doc.getString("teacherName") ?: ""
                val teacherEmail = doc.getString("teacherEmail") ?: ""
                val inviteCode = doc.getString("inviteCode") ?: ""

                val room = ClassRoom(
                    id = doc.id,
                    cityName = cityName,
                    districtName = districtName,
                    schoolName = schoolName,
                    gradeLevel = gradeLevel,
                    section = section,
                    teacherId = teacherId,
                    teacherName = teacherName,
                    teacherEmail = teacherEmail,
                    inviteCode = inviteCode,
                    isOccupied = true
                )
                if (_registeredClasses.value.none { it.id == doc.id }) {
                    _registeredClasses.value = _registeredClasses.value + room
                }
                room
            } else {
                _registeredClasses.value = _registeredClasses.value.filterNot { it.id == classroomId }
                null
            }
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, "classrooms/$classroomId")
            null
        }
    }

    private fun docToClassRoom(doc: com.google.firebase.firestore.DocumentSnapshot, fallbackUid: String, fallbackEmail: String): ClassRoom {
        return ClassRoom(
            id = doc.id,
            cityName = doc.getString("cityName") ?: "",
            districtName = doc.getString("districtName") ?: "",
            schoolName = doc.getString("schoolName") ?: "",
            gradeLevel = (doc.getLong("gradeLevel") ?: 1L).toInt(),
            section = doc.getString("section") ?: "A",
            teacherId = doc.getString("teacherId") ?: fallbackUid,
            teacherName = doc.getString("teacherName") ?: "",
            teacherEmail = doc.getString("teacherEmail") ?: fallbackEmail,
            inviteCode = doc.getString("inviteCode") ?: "",
            isOccupied = true
        )
    }

    suspend fun findTeacherClassroom(
        uid: String,
        teacherEmail: String,
        allowEmailFallbackForDifferentUid: Boolean = false
    ): ClassRoom? {
        if (Firebase.auth.currentUser == null) {
            return _registeredClasses.value.firstOrNull {
                it.teacherId == uid ||
                (allowEmailFallbackForDifferentUid && teacherEmail.isNotBlank() && it.teacherEmail.equals(teacherEmail.trim(), ignoreCase = true))
            }
        }

        val cleanEmail = teacherEmail.trim()

        // 1. Query Firestore classrooms from SERVER first by teacherId (& optionally teacherEmail)
        val parallelDoc = try {
            coroutineScope {
                val byUidDeferred = async {
                    withTimeoutOrNull(5000L) {
                        db.collection("classrooms").whereEqualTo("teacherId", uid).limit(1).get(Source.SERVER).await()
                    }?.documents?.firstOrNull()
                        ?: withTimeoutOrNull(2500L) {
                            db.collection("classrooms").whereEqualTo("teacherId", uid).limit(1).get().await()
                        }?.documents?.firstOrNull()
                }
                val byEmailDeferred = if (allowEmailFallbackForDifferentUid && cleanEmail.isNotBlank()) {
                    async {
                        withTimeoutOrNull(5000L) {
                            db.collection("classrooms").whereEqualTo("teacherEmail", cleanEmail).limit(1).get(Source.SERVER).await()
                        }?.documents?.firstOrNull()
                    }
                } else null
                byUidDeferred.await() ?: byEmailDeferred?.await()
            }
        } catch (_: Exception) { null }

        if (parallelDoc != null && parallelDoc.exists()) {
            val room = docToClassRoom(parallelDoc, uid, teacherEmail)
            if (_registeredClasses.value.none { it.id == parallelDoc.id }) {
                _registeredClasses.value = _registeredClasses.value + room
            }
            healTeacherUserAssignment(uid, room.id)
            return room
        }

        // 2. Fallback scan over all classrooms in Firestore
        try {
            val allClassrooms = withTimeoutOrNull(5000L) {
                db.collection("classrooms").get(Source.SERVER).await()
            } ?: withTimeoutOrNull(2500L) {
                db.collection("classrooms").get().await()
            }
            val matchedDoc = allClassrooms?.documents?.firstOrNull { doc ->
                val docTeacherId = doc.getString("teacherId") ?: ""
                val docEmail = doc.getString("teacherEmail") ?: ""
                (uid.isNotBlank() && docTeacherId == uid) ||
                (allowEmailFallbackForDifferentUid && cleanEmail.isNotBlank() && docEmail.trim().equals(cleanEmail, ignoreCase = true))
            }
            if (matchedDoc != null && matchedDoc.exists()) {
                val room = docToClassRoom(matchedDoc, uid, teacherEmail)
                if (_registeredClasses.value.none { it.id == matchedDoc.id }) {
                    _registeredClasses.value = _registeredClasses.value + room
                }
                healTeacherUserAssignment(uid, room.id)
                return room
            }
        } catch (_: Exception) {}

        return null
    }

    private fun healTeacherUserAssignment(uid: String, classId: String) {
        if (isAccountDeleting || isGlobalAccountDeleting) return
        if (uid.isBlank() || classId.isBlank() || Firebase.auth.currentUser == null) return
        scope.launch {
            if (isAccountDeleting || isGlobalAccountDeleting) return@launch
            try {
                db.collection("users").document(uid).update(
                    mapOf(
                        "assignedClassId" to classId,
                        "role" to "TEACHER",
                        "kvkkAccepted" to true,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                db.collection("classrooms").document(classId).update(
                    mapOf("teacherId" to uid)
                )
            } catch (_: Exception) {}
        }
    }

    suspend fun updateKvkkConsent(uid: String, accepted: Boolean) {
        if (isAccountDeleting || isGlobalAccountDeleting) return
        if (isFirebaseAuthAvailable) {
            val userRef = db.collection("users").document(uid)
            try {
                userRef.update(
                    mapOf(
                        "kvkkAccepted" to accepted,
                        "termsAccepted" to accepted,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.UPDATE, userRef.path)
            }
        }
    }

    private val isFirebaseAuthAvailable: Boolean
        get() = try {
            Firebase.auth.currentUser != null
        } catch (_: Throwable) {
            false
        }

    suspend fun updateUserRole(uid: String, role: String) {
        if (isAccountDeleting || isGlobalAccountDeleting) return
        if (isFirebaseAuthAvailable) {
            val userRef = db.collection("users").document(uid)
            val isActiveRole = role.equals("TEACHER", ignoreCase = true) ||
                role.equals("PARENT", ignoreCase = true) ||
                role.equals("OGRETMEN", ignoreCase = true) ||
                role.equals("VELI", ignoreCase = true)
            val payload = mutableMapOf<String, Any>(
                "role" to role,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (isActiveRole) {
                payload["kvkkAccepted"] = true
                payload["termsAccepted"] = true
            }
            try {
                userRef.update(payload).await()
            } catch (_: Exception) {
                // Silinmiş bir kullanıcı belgesini SetOptions.merge() ile tekrar oluşturma!
            }
        }
    }

    suspend fun findStudentsLinkedToParent(userEmail: String): List<Pair<Student, ClassRoom>> {
        val results = mutableListOf<Pair<Student, ClassRoom>>()
        val cleanEmail = userEmail.trim()
        if (cleanEmail.isBlank()) return results

        // 1. Check in-memory students
        val currentRoom = _registeredClasses.value.firstOrNull() ?: defaultSampleClass
        for (s in _students.value) {
            val matches = s.parentEmail?.trim().equals(cleanEmail, ignoreCase = true) ||
                    s.allLinkedParents.any { it.email.trim().equals(cleanEmail, ignoreCase = true) }
            if (matches) {
                results.add(Pair(s, currentRoom))
            }
        }

        // 2. Check sample students if not yet loaded in _students
        for (s in defaultSampleStudents) {
            val matches = s.parentEmail?.trim().equals(cleanEmail, ignoreCase = true) ||
                    s.allLinkedParents.any { it.email.trim().equals(cleanEmail, ignoreCase = true) }
            if (matches && results.none { it.first.id == s.id }) {
                results.add(Pair(s, defaultSampleClass))
            }
        }

        // 3. Check Firestore classrooms and students in parallel with bounded timeout
        if (isFirebaseAuthAvailable) {
            try {
                withTimeoutOrNull(6500L) {
                    val classSnap = withTimeoutOrNull(4000L) {
                        db.collection("classrooms").get().await()
                    } ?: withTimeoutOrNull(1000L) {
                        db.collection("classrooms").get(Source.CACHE).await()
                    } ?: return@withTimeoutOrNull

                    val matchesPerClass = coroutineScope {
                        classSnap.documents.map { cDoc ->
                            async {
                                val classId = cDoc.id
                                val studentsSnap = withTimeoutOrNull(3500L) {
                                    db.collection("classrooms").document(classId)
                                        .collection("students").get().await()
                                } ?: return@async emptyList<Pair<Student, ClassRoom>>()

                                val matched = mutableListOf<Pair<Student, ClassRoom>>()
                                for (sDoc in studentsSnap.documents) {
                                    val sParentEmail = sDoc.getString("parentEmail")
                                    @Suppress("UNCHECKED_CAST")
                                    val rawLinked = sDoc.get("linkedParents") as? List<Map<String, Any?>>
                                    val anyLinkedEmail = rawLinked?.any {
                                        (it["email"] as? String)?.trim().equals(cleanEmail, ignoreCase = true)
                                    } == true

                                    if (sParentEmail?.trim().equals(cleanEmail, ignoreCase = true) || anyLinkedEmail) {
                                        val sNo = (sDoc.getLong("studentNo") ?: 0L).toInt()
                                        val fullName = sDoc.getString("fullName") ?: ""
                                        val gender = sDoc.getString("gender") ?: "Erkek"
                                        val pCode = sDoc.getString("parentCode")
                                            ?: sDoc.getString("parentInviteCode")
                                            ?: ""
                                        val pName = sDoc.getString("parentName")
                                        val pLinkedAt = sDoc.getString("parentLinkedAt")
                                        val pLastLoginAt = sDoc.getString("parentLastLoginAt")
                                        val pIsOnline = sDoc.getBoolean("parentIsOnline") ?: false
                                        val pLastActiveMillis = sDoc.getLong("parentLastActiveMillis")

                                        val parsedLinked = rawLinked?.mapNotNull { map ->
                                            val e = map["email"] as? String
                                            val n = map["name"] as? String
                                            if (!e.isNullOrBlank() && !n.isNullOrBlank()) {
                                                LinkedParentAccount(
                                                    email = e,
                                                    name = n,
                                                    phone = (map["phone"] as? String) ?: "",
                                                    relationship = (map["relationship"] as? String) ?: "Veli",
                                                    linkedAt = (map["linkedAt"] as? String) ?: "Bağlandı",
                                                    lastLoginAt = map["lastLoginAt"] as? String,
                                                    isOnline = (map["isOnline"] as? Boolean) ?: pIsOnline,
                                                    lastActiveMillis = (map["lastActiveMillis"] as? Number)?.toLong() ?: pLastActiveMillis
                                                )
                                            } else null
                                        } ?: emptyList()

                                        val student = Student(
                                            id = sDoc.id,
                                            studentNumber = sNo.toString(),
                                            fullName = fullName,
                                            gender = gender,
                                            parentInviteCode = pCode,
                                            parentName = pName,
                                            parentEmail = sParentEmail,
                                            parentLinkedAt = pLinkedAt,
                                            parentLastLoginAt = pLastLoginAt,
                                            parentIsOnline = pIsOnline,
                                            parentLastActiveMillis = pLastActiveMillis,
                                            linkedParents = parsedLinked
                                        )

                                        val classRoom = ClassRoom(
                                            id = cDoc.id,
                                            cityName = cDoc.getString("cityName") ?: "",
                                            districtName = cDoc.getString("districtName") ?: "",
                                            schoolName = cDoc.getString("schoolName") ?: "",
                                            gradeLevel = (cDoc.getLong("gradeLevel") ?: 1L).toInt(),
                                            section = cDoc.getString("section") ?: "A",
                                            teacherName = cDoc.getString("teacherName") ?: "",
                                            teacherEmail = cDoc.getString("teacherEmail") ?: "",
                                            inviteCode = cDoc.getString("inviteCode") ?: "",
                                            isOccupied = true
                                        )
                                        matched.add(Pair(student, classRoom))
                                    }
                                }
                                matched
                            }
                        }.awaitAll()
                    }

                    matchesPerClass.flatten().forEach { pair ->
                        if (results.none { it.first.id == pair.first.id }) {
                            results.add(pair)
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        return results
    }

    suspend fun saveFcmToken(uid: String, token: String) {
        if (isAccountDeleting || isGlobalAccountDeleting) return
        if (uid.isBlank() || token.isBlank()) return
        try {
            db.collection("users").document(uid)
                .update(
                    mapOf(
                        "fcmToken" to token,
                        "fcmUpdatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
        } catch (_: Exception) {
        }
    }

    /**
     * Web sisteminden aktarılan öğrencileri mevcut sınıftaki öğrencilerle (varsa bağlı velilerini ve ID'lerini koruyarak)
     * birleştirir, hem yerel _students StateFlow'una hem de Firestore'a toplu olarak yazar.
     */
    suspend fun syncMigratedStudentsToClassroom(
        classroomId: String,
        incomingStudents: List<Student>
    ): List<Student> {
        if (incomingStudents.isEmpty()) return _students.value

        val existingByNo = mutableMapOf<String, Student>()
        val duplicateDocIdsToRemove = mutableSetOf<String>()

        // 1. Önce hafızadaki mevcut öğrencileri al
        _students.value.forEach { st ->
            val no = st.studentNumber.trim()
            if (no.isNotBlank()) {
                val prev = existingByNo[no]
                if (prev == null || (!prev.isParentLinked && st.isParentLinked)) {
                    existingByNo[no] = st
                }
            }
        }

        // 2. Firebase oturumu açıksa Firestore'daki mevcut öğrenci belgelerini de kontrol et (bağlı veli ve belge ID'sini korumak için)
        if (classroomId.isNotBlank() && classroomId != "default" && isFirebaseAuthAvailable) {
            try {
                val snap = db.collection("classrooms").document(classroomId).collection("students").get().await()
                for (doc in snap.documents) {
                    val parsed = parseStudentFromDoc(doc, fallbackCode = TurkishTextHelper.generateVeliKatilimKodu())
                    val no = parsed.studentNumber.trim()
                    if (no.isBlank() || no == "0") continue
                    val prev = existingByNo[no]
                    if (prev == null) {
                        existingByNo[no] = parsed
                    } else if (!prev.isParentLinked && parsed.isParentLinked) {
                        if (prev.id != parsed.id) duplicateDocIdsToRemove.add(prev.id)
                        existingByNo[no] = parsed
                    } else if (prev.id != parsed.id) {
                        duplicateDocIdsToRemove.add(parsed.id)
                    }
                }
            } catch (_: Exception) {
            }
        }

        val sampleCodesByNo = defaultSampleStudents.associate { it.studentNumber.trim() to it.parentInviteCode }
        val resolvedStudents = incomingStudents.map { incoming ->
            val no = incoming.studentNumber.trim()
            val existing = existingByNo[no]
            val finalId = existing?.id?.takeIf { it.isNotBlank() } ?: incoming.id.ifBlank { "std_$no" }
            val finalCode = existing?.parentInviteCode?.takeIf { it.isNotBlank() }
                ?: sampleCodesByNo[no]?.takeIf { it.isNotBlank() }
                ?: incoming.parentInviteCode.ifBlank { TurkishTextHelper.generateVeliKatilimKodu() }

            Student(
                id = finalId,
                studentNumber = no,
                fullName = TurkishTextHelper.toTurkishUpperCase(incoming.fullName.trim()),
                gender = if (incoming.gender.equals("Kız", ignoreCase = true)) "Kız" else "Erkek",
                parentInviteCode = finalCode,
                isPriority = incoming.isPriority || (existing?.isPriority == true),
                isFixed = incoming.isFixed || (existing?.isFixed == true),
                parentName = existing?.parentName,
                parentEmail = existing?.parentEmail,
                parentLinkedAt = existing?.parentLinkedAt,
                parentLastLoginAt = existing?.parentLastLoginAt,
                parentIsOnline = existing?.parentIsOnline ?: false,
                parentLastActiveMillis = existing?.parentLastActiveMillis,
                linkedParents = existing?.linkedParents ?: emptyList()
            )
        }.sortedBy { it.studentNumber.toIntOrNull() ?: 9999 }

        _students.value = resolvedStudents

        if (classroomId.isNotBlank() && classroomId != "default" && isFirebaseAuthAvailable) {
            try {
                val studentsCol = db.collection("classrooms").document(classroomId).collection("students")
                val batch = db.batch()
                resolvedStudents.forEach { st ->
                    val docRef = studentsCol.document(st.id)
                    val payload = mutableMapOf<String, Any?>(
                        "studentNo" to (st.studentNumber.toIntOrNull() ?: 0),
                        "fullName" to st.fullName,
                        "gender" to st.gender,
                        "parentCode" to st.parentInviteCode,
                        "parentInviteCode" to st.parentInviteCode,
                        "isPriority" to st.isPriority,
                        "isFixed" to st.isFixed,
                        "classroomId" to classroomId,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    if (!st.parentEmail.isNullOrBlank()) payload["parentEmail"] = st.parentEmail
                    if (!st.parentName.isNullOrBlank()) payload["parentName"] = st.parentName
                    if (!st.parentLinkedAt.isNullOrBlank()) payload["parentLinkedAt"] = st.parentLinkedAt
                    if (!st.parentLastLoginAt.isNullOrBlank()) payload["parentLastLoginAt"] = st.parentLastLoginAt
                    if (st.linkedParents.isNotEmpty()) {
                        payload["linkedParents"] = st.linkedParents.map { p ->
                            mapOf(
                                "email" to p.email,
                                "name" to p.name,
                                "phone" to p.phone,
                                "relationship" to p.relationship,
                                "linkedAt" to p.linkedAt,
                                "lastLoginAt" to p.lastLoginAt,
                                "isOnline" to p.isOnline,
                                "lastActiveMillis" to p.lastActiveMillis
                            )
                        }
                    }
                    batch.set(docRef, payload, com.google.firebase.firestore.SetOptions.merge())
                }
                duplicateDocIdsToRemove.forEach { dupId ->
                    if (dupId.isNotBlank() && resolvedStudents.none { it.id == dupId }) {
                        batch.delete(studentsCol.document(dupId))
                    }
                }
                batch.set(
                    db.collection("classrooms").document(classroomId),
                    mapOf(
                        "studentCount" to resolvedStudents.size,
                        "updatedAt" to FieldValue.serverTimestamp()
                    ),
                    com.google.firebase.firestore.SetOptions.merge()
                )
                batch.commit().await()
            } catch (_: Exception) {
            }
        }

        return resolvedStudents
    }

    companion object {
        @Volatile
        var isGlobalAccountDeleting: Boolean = false

        private const val DIRECT_REST_PROJECT_ID = "gen-lang-client-0343498023"
        private const val DIRECT_REST_DB_ID = "ai-studio-android-biriz-caa1dae7-0ec2-4613-b717-5606838b083e"

        // Firebase Authentication'dan silinmiş olduğu halde önceki sürümlerde Firestore `users`
        // koleksiyonunda yetim kalmış olan geçersiz kullanıcı belge ID'leri
        private val KNOWN_ORPHANED_USER_DOC_IDS = setOf(
            "89IbYWXBr6XVZcjw1oCQbhoPF3t1",
            "J86gvskF6RhvZyCv8xu5TEfj4xY2",
            "XzdEVxDmrWRmUxYJvt81JIeQ7J42",
            "dfR2XykVw8XGPzaaXHDqNW20B393"
        )

        private val directRestHttpClient: okhttp3.OkHttpClient by lazy {
            okhttp3.OkHttpClient.Builder()
                .connectTimeout(5, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(6, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(6, java.util.concurrent.TimeUnit.SECONDS)
                .build()
        }

        internal fun parseLinkedParentsFromDoc(
            doc: com.google.firebase.firestore.DocumentSnapshot,
            includeLegacyFallback: Boolean = true
        ): List<LinkedParentAccount> {
            val parentName = doc.getString("parentName")?.trim()
            val parentEmail = doc.getString("parentEmail")?.trim()
            val parentLinkedAt = doc.getString("parentLinkedAt")
            val parentLastLoginAt = doc.getString("parentLastLoginAt")
            val hasTopOnlineField = doc.contains("parentIsOnline")
            val parentIsOnline = doc.getBoolean("parentIsOnline") ?: false
            val parentLastActiveMillis = doc.getLong("parentLastActiveMillis")

            @Suppress("UNCHECKED_CAST")
            val rawLinkedParents = doc.get("linkedParents") as? List<Map<String, Any?>>
            val parsedLinked = rawLinkedParents?.mapNotNull { map ->
                val e = (map["email"] as? String)?.trim()
                val n = (map["name"] as? String)?.trim()
                if (!e.isNullOrBlank() && !n.isNullOrBlank()) {
                    val singleParent = rawLinkedParents.size <= 1
                    val pOnline = (map["isOnline"] as? Boolean)
                        ?: if (hasTopOnlineField && singleParent) parentIsOnline else false
                    val pActiveMillis = (map["lastActiveMillis"] as? Number)?.toLong()
                        ?: if (singleParent) parentLastActiveMillis else null
                    val pLastLogin = (map["lastLoginAt"] as? String)
                        ?: if (singleParent) parentLastLoginAt else null
                    LinkedParentAccount(
                        email = e,
                        name = n,
                        phone = (map["phone"] as? String) ?: "",
                        relationship = (map["relationship"] as? String) ?: "Veli",
                        linkedAt = (map["linkedAt"] as? String) ?: "Bağlandı",
                        lastLoginAt = pLastLogin,
                        isOnline = pOnline,
                        lastActiveMillis = pActiveMillis
                    )
                } else null
            } ?: emptyList()

            if (!includeLegacyFallback || parsedLinked.isNotEmpty() || parentEmail.isNullOrBlank()) {
                return parsedLinked
            }
            return listOf(
                LinkedParentAccount(
                    email = parentEmail,
                    name = parentName?.takeIf { it.isNotBlank() } ?: "Öğrenci Velisi",
                    phone = "",
                    relationship = "Veli",
                    linkedAt = parentLinkedAt ?: "Bağlandı",
                    lastLoginAt = parentLastLoginAt,
                    isOnline = parentIsOnline,
                    lastActiveMillis = parentLastActiveMillis
                )
            )
        }

        internal fun parseStudentFromDoc(
            doc: com.google.firebase.firestore.DocumentSnapshot,
            fallbackCode: String = ""
        ): Student {
            val studentNo = (doc.getLong("studentNo") ?: 0L).toInt()
            val fullName = doc.getString("fullName") ?: ""
            val gender = doc.getString("gender") ?: "Erkek"
            val rawCode = doc.getString("parentCode")
                ?: doc.getString("parentInviteCode")
                ?: doc.getString("veliKodu")
                ?: doc.getString("code")
                ?: fallbackCode
            return Student(
                id = doc.id,
                studentNumber = studentNo.toString(),
                fullName = fullName,
                gender = gender,
                parentInviteCode = rawCode,
                parentName = doc.getString("parentName"),
                parentEmail = doc.getString("parentEmail"),
                parentLinkedAt = doc.getString("parentLinkedAt"),
                parentLastLoginAt = doc.getString("parentLastLoginAt"),
                parentIsOnline = doc.getBoolean("parentIsOnline") ?: false,
                parentLastActiveMillis = doc.getLong("parentLastActiveMillis"),
                linkedParents = parseLinkedParentsFromDoc(doc, includeLegacyFallback = false)
            )
        }
    }
}
