package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val fullName: String = "",
    val email: String = "",
    val role: String = "MEMBER",
    val kvkkAccepted: Boolean = false,
    val assignedSchoolId: String? = null,
    val assignedClassId: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ClassRoomDocument(
    @DocumentId val id: String = "",
    val cityId: String = "",
    val cityName: String = "",
    val districtName: String = "",
    val schoolName: String = "",
    val gradeLevel: Int = 1,
    val section: String = "",
    val teacherId: String = "",
    val teacherName: String = "",
    val teacherEmail: String = "",
    val inviteCode: String = "",
    val studentCount: Int = 0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class SchoolDocument(
    @DocumentId val id: String = "",
    val cityId: String = "",
    val districtName: String = "",
    val name: String = "",
    val creatorId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class StudentDocument(
    @DocumentId val id: String = "",
    val classroomId: String = "",
    val studentNo: Int = 0,
    val fullName: String = "",
    val gender: String = "Erkek",
    val parentCode: String = "",
    val parentName: String? = null,
    val parentEmail: String? = null,
    val parentLinkedAt: String? = null,
    val parentLastLoginAt: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
