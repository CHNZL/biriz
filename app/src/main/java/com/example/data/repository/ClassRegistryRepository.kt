package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.BranchStatus
import com.example.data.model.City
import com.example.data.model.ClassRoom
import com.example.data.model.ClassRoomDocument
import com.example.data.model.LinkedParentAccount
import com.example.data.model.School
import com.example.data.model.SchoolDocument
import com.example.data.model.Student
import com.example.util.OperationType
import com.example.util.TurkishTextHelper
import com.example.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.tasks.await

class ClassRegistryRepository(
    private val db: FirebaseFirestore,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    // Secondary constructor requiring explicit firestore_database_id
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    val cities: List<City> = TurkeyGeoRepository.allCities

    private val _schools = MutableStateFlow<List<School>>(emptyList())
    val schools: StateFlow<List<School>> = _schools.asStateFlow()

    private val _registeredClasses = MutableStateFlow<List<ClassRoom>>(emptyList())
    val registeredClasses: StateFlow<List<ClassRoom>> = _registeredClasses.asStateFlow()

    val allSections: List<String> = ('A'..'Z').map { it.toString() }

    private var classroomsJob: kotlinx.coroutines.Job? = null
    private var schoolsJob: kotlinx.coroutines.Job? = null

    init {
        Firebase.auth.addAuthStateListener { auth ->
            val user = auth.currentUser
            if (user != null) {
                startObserving()
            } else {
                stopObserving()
            }
        }
        if (Firebase.auth.currentUser != null) {
            startObserving()
        }
    }

    private fun startObserving() {
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
                        val teacherName = doc.getString("teacherName") ?: ""
                        val teacherEmail = doc.getString("teacherEmail") ?: ""
                        val inviteCode = doc.getString("inviteCode") ?: ""

                        ClassRoom(
                            id = doc.id,
                            cityName = cityName,
                            districtName = districtName,
                            schoolName = schoolName,
                            gradeLevel = gradeLevel,
                            section = section,
                            teacherName = teacherName,
                            teacherEmail = teacherEmail,
                            inviteCode = inviteCode,
                            isOccupied = true
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

    fun seedDefaultTestClassroom(userId: String = "test_teacher_preview_uid"): ClassRoom {
        val defaultClass = ClassRoom(
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

        val sampleStudents = listOf(
            Student(
                id = "std_45",
                studentNumber = "45",
                fullName = "SEMİH AHMET KAVAKOĞLU",
                gender = "Erkek",
                parentInviteCode = "BRZ-KAV-45",
                parentName = "Ahmet Kavakoğlu",
                parentEmail = "ahmet.kav@gmail.com",
                parentLinkedAt = "12 Ekim 2026, 14:20",
                parentLastLoginAt = "Bugün, 09:15",
                linkedParents = listOf(
                    LinkedParentAccount(
                        email = "ahmet.kav@gmail.com",
                        name = "Ahmet Kavakoğlu",
                        phone = "0532 111 22 33",
                        relationship = "Baba",
                        linkedAt = "12 Ekim 2026, 14:20",
                        lastLoginAt = "Bugün, 09:15"
                    ),
                    LinkedParentAccount(
                        email = "zeynep.kav@gmail.com",
                        name = "Zeynep Kavakoğlu",
                        phone = "0533 222 33 44",
                        relationship = "Anne",
                        linkedAt = "14 Ekim 2026, 11:05",
                        lastLoginAt = "Dün, 18:40"
                    )
                )
            ),
            Student(id = "std_326", studentNumber = "326", fullName = "GÜNEŞ DEMİR", gender = "Kız", parentInviteCode = "BRZ-DMR-326", isPriority = true),
            Student(id = "std_402", studentNumber = "402", fullName = "AHMET ASAF IŞIK", gender = "Erkek", parentInviteCode = "BRZ-ISK-402"),
            Student(id = "std_407", studentNumber = "407", fullName = "EMİR ASAF AKKAŞ", gender = "Erkek", parentInviteCode = "BRZ-AKK-407"),
            Student(
                id = "std_408",
                studentNumber = "408",
                fullName = "KEVSER ÖZDEMİR",
                gender = "Kız",
                parentInviteCode = "BRZ-OZD-408",
                parentName = "Fatma Özdemir",
                parentEmail = "fatma.ozdemir@gmail.com",
                parentLinkedAt = "18 Ekim 2026, 10:00",
                parentLastLoginAt = "Bugün, 08:30",
                linkedParents = listOf(
                    LinkedParentAccount(
                        email = "fatma.ozdemir@gmail.com",
                        name = "Fatma Özdemir",
                        phone = "0555 333 44 55",
                        relationship = "Anne",
                        linkedAt = "18 Ekim 2026, 10:00",
                        lastLoginAt = "Bugün, 08:30"
                    )
                )
            ),
            Student(id = "std_409", studentNumber = "409", fullName = "MEHMET AKİF KILIÇ", gender = "Erkek", parentInviteCode = "BRZ-KLC-409"),
            Student(id = "std_410", studentNumber = "410", fullName = "ELİF ZÜLAL YILMAZ", gender = "Kız", parentInviteCode = "BRZ-YLM-410"),
            Student(id = "std_412", studentNumber = "412", fullName = "YUSUF KEREM KAYA", gender = "Erkek", parentInviteCode = "BRZ-KYA-412"),
            Student(id = "std_415", studentNumber = "415", fullName = "ZEYNEP MİRA ÇETİN", gender = "Kız", parentInviteCode = "BRZ-CTN-415"),
            Student(id = "std_418", studentNumber = "418", fullName = "YAĞIZ EFE DEMİRTAŞ", gender = "Erkek", parentInviteCode = "BRZ-DMT-418"),
            Student(id = "std_420", studentNumber = "420", fullName = "ASYA ÇELİK", gender = "Kız", parentInviteCode = "BRZ-CLK-420"),
            Student(id = "std_422", studentNumber = "422", fullName = "MEHMET EREN ŞAHİN", gender = "Erkek", parentInviteCode = "BRZ-SHN-422"),
            Student(id = "std_425", studentNumber = "425", fullName = "ECRİN NAZLI KOÇ", gender = "Kız", parentInviteCode = "BRZ-KOC-425"),
            Student(id = "std_428", studentNumber = "428", fullName = "MUSTAFA BURAK ARSLAN", gender = "Erkek", parentInviteCode = "BRZ-ARS-428"),
            Student(id = "std_430", studentNumber = "430", fullName = "DEFNE ÖZTÜRK", gender = "Kız", parentInviteCode = "BRZ-OZT-430"),
            Student(id = "std_433", studentNumber = "433", fullName = "BERAT ALİ YILDIZ", gender = "Erkek", parentInviteCode = "BRZ-YLD-433"),
            Student(id = "std_435", studentNumber = "435", fullName = "MERYEM ADA AYDIN", gender = "Kız", parentInviteCode = "BRZ-AYD-435"),
            Student(id = "std_438", studentNumber = "438", fullName = "ENES TALHA DOĞAN", gender = "Erkek", parentInviteCode = "BRZ-DGN-438"),
            Student(id = "std_440", studentNumber = "440", fullName = "HİRANUR POLAT", gender = "Kız", parentInviteCode = "BRZ-PLT-440"),
            Student(id = "std_442", studentNumber = "442", fullName = "ÖMER ASAF GÜLER", gender = "Erkek", parentInviteCode = "BRZ-GLR-442"),
            Student(id = "std_445", studentNumber = "445", fullName = "BEREN SU ÇAKIR", gender = "Kız", parentInviteCode = "BRZ-CKR-445"),
            Student(id = "std_448", studentNumber = "448", fullName = "ALPEREN KILIÇASLAN", gender = "Erkek", parentInviteCode = "BRZ-KLC-448"),
            Student(id = "std_450", studentNumber = "450", fullName = "ZEHRA BETÜL KORKMAZ", gender = "Kız", parentInviteCode = "BRZ-KRK-450"),
            Student(id = "std_452", studentNumber = "452", fullName = "BATUHAN YAVUZ", gender = "Erkek", parentInviteCode = "BRZ-YVZ-452"),
            Student(id = "std_455", studentNumber = "455", fullName = "EYLÜL SARE TAŞKIN", gender = "Kız", parentInviteCode = "BRZ-TSK-455")
        )

        _students.value = sampleStudents
        _registeredClasses.value = _registeredClasses.value.filter { it.id != defaultClass.id } + defaultClass
        return defaultClass
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

        _registeredClasses.value = _registeredClasses.value.filter { it.id != docId } + classRoom
        return classRoom
    }

    // --- Öğrenci Listesi Yönetimi (Firestore Subcollection) ---

    private val _students = MutableStateFlow<List<Student>>(emptyList())
    val students: StateFlow<List<Student>> = _students.asStateFlow()

    private var studentsJob: kotlinx.coroutines.Job? = null

    fun observeStudents(classroomId: String) {
        studentsJob?.cancel()
        if (Firebase.auth.currentUser != null) {
            studentsJob = db.collection("classrooms").document(classroomId).collection("students")
                .snapshots()
                .onEach { snapshot ->
                    val list = snapshot.documents.mapNotNull { doc ->
                        val studentNo = (doc.getLong("studentNo") ?: 0L).toInt()
                        val fullName = doc.getString("fullName") ?: return@mapNotNull null
                        val gender = doc.getString("gender") ?: "Erkek"
                        val parentCode = doc.getString("parentCode") ?: ""
                        val parentName = doc.getString("parentName")
                        val parentEmail = doc.getString("parentEmail")
                        val parentLinkedAt = doc.getString("parentLinkedAt")
                        val parentLastLoginAt = doc.getString("parentLastLoginAt")
                        @Suppress("UNCHECKED_CAST")
                        val rawLinkedParents = doc.get("linkedParents") as? List<Map<String, Any?>>
                        val parsedLinked = rawLinkedParents?.mapNotNull { map ->
                            val e = map["email"] as? String
                            val n = map["name"] as? String
                            if (!e.isNullOrBlank() && !n.isNullOrBlank()) {
                                LinkedParentAccount(
                                    email = e,
                                    name = n,
                                    phone = (map["phone"] as? String) ?: "",
                                    relationship = (map["relationship"] as? String) ?: "Veli",
                                    linkedAt = (map["linkedAt"] as? String) ?: "Bağlandı",
                                    lastLoginAt = map["lastLoginAt"] as? String
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
        if (Firebase.auth.currentUser != null) {
            val studentRef = db.collection("classrooms").document(classroomId).collection("students").document(studentId)
            val updates = mutableMapOf<String, Any?>(
                "parentName" to parentName,
                "parentEmail" to parentEmail,
                "parentLinkedAt" to parentLinkedAt,
                "parentLastLoginAt" to parentLastLoginAt,
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
                    parentName = parentName,
                    parentEmail = parentEmail,
                    parentLinkedAt = parentLinkedAt,
                    parentLastLoginAt = parentLastLoginAt
                )
            } else current
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

    suspend fun deleteTeacherRoleAndClassroom(classroomId: String) {
        val uid = requireUserId()
        if (Firebase.auth.currentUser != null) {
            try {
                val studentSnapshots = db.collection("classrooms").document(classroomId)
                    .collection("students").get().await()
                for (doc in studentSnapshots.documents) {
                    doc.reference.delete().await()
                }
            } catch (e: Exception) {
                // ignore
            }

            val classRef = db.collection("classrooms").document(classroomId)
            try {
                classRef.delete().await()
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.DELETE, classRef.path)
                throw e
            }

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

    suspend fun deleteEntireAccount(classroomId: String?) {
        val uid = requireUserId()
        if (Firebase.auth.currentUser != null) {
            if (!classroomId.isNullOrBlank()) {
                try {
                    val studentSnapshots = db.collection("classrooms").document(classroomId)
                        .collection("students").get().await()
                    for (doc in studentSnapshots.documents) {
                        doc.reference.delete().await()
                    }
                    db.collection("classrooms").document(classroomId).delete().await()
                } catch (e: Exception) {
                    // ignore
                }
            }

            val userRef = db.collection("users").document(uid)
            try {
                userRef.delete().await()
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.DELETE, userRef.path)
            }

            try {
                Firebase.auth.currentUser?.delete()?.await()
            } catch (e: Exception) {
                // ignore
            }
        }

        if (!classroomId.isNullOrBlank()) {
            _registeredClasses.value = _registeredClasses.value.filter { it.id != classroomId }
        }
        _students.value = emptyList()
        disableTestMode()
    }

    suspend fun ensureUserProfile(uid: String, fullName: String, email: String): com.example.data.model.UserProfile {
        if (Firebase.auth.currentUser == null) {
            return com.example.data.model.UserProfile(
                id = uid,
                userId = uid,
                fullName = fullName,
                email = email,
                role = "UYE",
                kvkkAccepted = false
            )
        }

        val userRef = db.collection("users").document(uid)
        val snapshot = try {
            userRef.get().await()
        } catch (e: Exception) {
            null
        }

        if (snapshot != null && snapshot.exists()) {
            return com.example.data.model.UserProfile(
                id = snapshot.id,
                userId = snapshot.getString("userId") ?: uid,
                fullName = snapshot.getString("fullName") ?: fullName,
                email = snapshot.getString("email") ?: email,
                role = snapshot.getString("role") ?: "UYE",
                kvkkAccepted = snapshot.getBoolean("kvkkAccepted") ?: false,
                assignedClassId = snapshot.getString("assignedClassId"),
                assignedSchoolId = snapshot.getString("assignedSchoolId")
            )
        }

        val payload = mapOf(
            "userId" to uid,
            "fullName" to fullName,
            "email" to email,
            "role" to "UYE",
            "kvkkAccepted" to false,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            userRef.set(payload).await()
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, userRef.path)
        }

        return com.example.data.model.UserProfile(
            id = uid,
            userId = uid,
            fullName = fullName,
            email = email,
            role = "UYE",
            kvkkAccepted = false
        )
    }

    suspend fun updateKvkkConsent(uid: String, accepted: Boolean) {
        if (Firebase.auth.currentUser != null) {
            val userRef = db.collection("users").document(uid)
            try {
                userRef.update(
                    mapOf(
                        "kvkkAccepted" to accepted,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.UPDATE, userRef.path)
            }
        }
    }
}
