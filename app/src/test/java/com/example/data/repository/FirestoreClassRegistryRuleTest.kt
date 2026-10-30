package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.ClassRoomDocument
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FirestoreClassRegistryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun testTeacherCanCreateAndReadClassroom() = runBlocking {
        org.junit.Assume.assumeTrue("Local Firestore/Auth emulator must be active", isEmulatorAvailable())
        val uid = signInTestUser("teacher_alice@meb.k12.tr")
        assertNotNull("User UID must be present", uid)

        val docRef = firestore.collection("classrooms").document("sivas_merkez_cumhuriyet_1a")
        val payload = mapOf(
            "cityId" to "58",
            "cityName" to "SİVAS",
            "districtName" to "MERKEZ",
            "schoolName" to "CUMHURİYET İLKOKULU",
            "gradeLevel" to 1,
            "section" to "A",
            "teacherId" to uid,
            "teacherName" to "Alice Öğretmen",
            "teacherEmail" to "teacher_alice@meb.k12.tr",
            "inviteCode" to "BRZ-1A-9901",
            "studentCount" to 0,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        docRef.set(payload).await()

        val snapshot = docRef.get().await()
        assertTrue("Classroom document should exist", snapshot.exists())
        assertEquals("SİVAS", snapshot.getString("cityName"))
        assertEquals("CUMHURİYET İLKOKULU", snapshot.getString("schoolName"))
    }
}
