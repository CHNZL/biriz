package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.repository.TurkeyGeoRepository
import com.example.util.TurkishTextHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("BİRİZ", appName)
    }

    @Test
    fun `test turkish uppercase conversion with i and dotless i`() {
        val input = "atatürk ilkokulu ışık ilköğretim şırnak"
        val uppercase = TurkishTextHelper.toTurkishUpperCase(input)
        assertEquals("ATATÜRK İLKOKULU IŞIK İLKÖĞRETİM ŞIRNAK", uppercase)
    }

    @Test
    fun `render official logo to icon`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val drawable = androidx.core.content.ContextCompat.getDrawable(context, R.drawable.ic_biriz_logo)
        assertNotNull(drawable)
        val bitmap = android.graphics.Bitmap.createBitmap(512, 512, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        val bgPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
        }
        canvas.drawRect(0f, 0f, 512f, 512f, bgPaint)
        drawable?.setBounds(32, 32, 480, 480)
        drawable?.draw(canvas)
        java.io.File("/tmp/biriz_official_icon.png").outputStream().use { out ->
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
        }
        assertTrue(java.io.File("/tmp/biriz_official_icon.png").exists())
    }

    @Test
    fun `test 81 provinces in turkey and alphabetical ordering`() {
        val cities = TurkeyGeoRepository.allCities
        assertEquals(81, cities.size)
        assertEquals("Adana", cities.first().name)
        assertEquals("Zonguldak", cities.last().name)
        // Verify districts of Sivas
        val sivas = cities.first { it.name == "Sivas" }
        assertTrue(sivas.districts.contains("Merkez"))
        assertTrue(sivas.districts.contains("Şarkışla"))
    }

    @Test
    fun `test schedule persistence helper save and load`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val classId = "test_class_4d"
        val customConfig = com.example.data.model.ScheduleConfig(
            dailyLessonCount = 7,
            firstLessonStartTime = "09:00",
            lessonDurationMinutes = 45,
            recessDurations = listOf(10, 10, 15, 10, 10, 10),
            isLunchBreakActive = true,
            lunchAfterLessonIndex = 4,
            lunchDurationMinutes = 40,
            isConfigured = true
        )

        com.example.util.SchedulePersistenceHelper.saveScheduleConfig(context, classId, customConfig)
        val loadedConfig = com.example.util.SchedulePersistenceHelper.loadScheduleConfig(context, classId)

        assertNotNull(loadedConfig)
        assertEquals(7, loadedConfig?.dailyLessonCount)
        assertEquals("09:00", loadedConfig?.firstLessonStartTime)
        assertEquals(45, loadedConfig?.lessonDurationMinutes)
        assertEquals(listOf(10, 10, 15, 10, 10, 10), loadedConfig?.recessDurations)
        assertEquals(true, loadedConfig?.isLunchBreakActive)
        assertEquals(4, loadedConfig?.lunchAfterLessonIndex)
        assertEquals(40, loadedConfig?.lunchDurationMinutes)

        // Test lesson persistence
        val customLessons = listOf(
            com.example.data.model.ScheduleLesson(
                dayOfWeek = "PZT",
                lessonIndex = 1,
                subjectName = "TÜRKÇE",
                colorTheme = "TURQUOISE"
            ),
            com.example.data.model.ScheduleLesson(
                dayOfWeek = "PZT",
                lessonIndex = 2,
                subjectName = "MATEMATİK",
                colorTheme = "AMBER"
            )
        )
        com.example.util.SchedulePersistenceHelper.saveScheduleLessons(context, classId, customLessons)
        val loadedLessons = com.example.util.SchedulePersistenceHelper.loadScheduleLessons(context, classId)

        assertNotNull(loadedLessons)
        assertEquals(2, loadedLessons?.size)
        assertEquals("TÜRKÇE", loadedLessons?.get(0)?.subjectName)
        assertEquals("TURQUOISE", loadedLessons?.get(0)?.colorTheme)
        assertEquals("MATEMATİK", loadedLessons?.get(1)?.subjectName)

        // Test active day persistence
        com.example.util.SchedulePersistenceHelper.saveActiveDay(context, classId, "ÇAR")
        val loadedDay = com.example.util.SchedulePersistenceHelper.loadActiveDay(context, classId)
        assertEquals("ÇAR", loadedDay)
    }

    @Test
    fun `test qr code bitmap generation`() {
        val bitmap = com.example.util.QrCodeHelper.generateQrBitmap(
            content = com.example.util.QrCodeHelper.LATEST_APK_DOWNLOAD_URL,
            size = 200
        )
        assertNotNull(bitmap)
        assertEquals(200, bitmap?.width)
        assertEquals(200, bitmap?.height)
    }

    @Test
    fun `test tournament pdf export succeeds for all four formats`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val students = (1..32).map {
            com.example.data.model.Student(
                id = "s$it",
                studentNumber = "$it",
                fullName = "Öğrenci $it",
                gender = if (it % 2 == 0) "Kız" else "Erkek"
            )
        }
        val pairs8 = students.take(8).map { Pair(it.id, it.fullName) }
        val pairs32 = students.map { Pair(it.id, it.fullName) }

        class RecordingCanvasPdfSink : com.example.util.TournamentPdfHelper.PdfPageSink {
            val pages = mutableListOf<Triple<Int, Int, Int>>() // width, height, pageNumber
            var activeCount = 0
            var wroteBytes = false
            var closed = false

            override fun startPage(width: Int, height: Int, pageNumber: Int): android.graphics.Canvas {
                assertEquals("Previous page must be finished before starting page $pageNumber", 0, activeCount)
                activeCount++
                pages.add(Triple(width, height, pageNumber))
                val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
                return android.graphics.Canvas(bitmap)
            }

            override fun finishCurrentPage() {
                assertEquals("Must have an active page to finish", 1, activeCount)
                activeCount--
            }

            override fun writeTo(out: java.io.OutputStream) {
                assertEquals("All pages must be finished before writing PDF", 0, activeCount)
                out.write("%PDF-1.4\n".toByteArray())
                wroteBytes = true
            }

            override fun close() {
                closed = true
            }
        }

        // 1. ELEME (8 players) — triggers bracket page + fixture table page (previously crashed on page 2!)
        val koTournament = com.example.data.model.Tournament(
            id = "t/eleme:test",
            classRoomId = "c1",
            name = "Akıl ve Zeka Oyunları Turnuvası",
            type = com.example.data.model.TournamentType.ELEME,
            status = com.example.data.model.TournamentStatus.DEVAM_EDIYOR,
            participants = students.take(8).map { it.id }
        )
        val koMatches = com.example.util.TournamentEngine.generateKnockoutFixture(
            tournamentId = koTournament.id,
            participants = pairs8,
            isDoubleMatch = false
        )
        val koSink = RecordingCanvasPdfSink()
        val koFile = com.example.util.TournamentPdfHelper.exportTournamentPdfToFile(context, koTournament, koMatches, emptyList(), students, koSink)
        assertTrue(koFile.exists())
        assertTrue(koSink.wroteBytes && koSink.closed)
        assertEquals(0, koSink.activeCount)
        assertTrue("ELEME PDF must have >= 2 sequential pages", koSink.pages.size >= 2)
        assertEquals(listOf(1, 2), koSink.pages.take(2).map { it.third })

        // 1b. Large ELEME (32 players -> 16 R1 matches -> overflow first round + main bracket + table)
        val koLargeTournament = koTournament.copy(id = "t_eleme_32", participants = students.map { it.id })
        val koLargeMatches = com.example.util.TournamentEngine.generateKnockoutFixture("t_eleme_32", pairs32, false)
        val koLargeSink = RecordingCanvasPdfSink()
        com.example.util.TournamentPdfHelper.exportTournamentPdfToFile(context, koLargeTournament, koLargeMatches, emptyList(), students, koLargeSink)
        assertTrue(koLargeSink.wroteBytes && koLargeSink.closed)
        assertEquals((1..koLargeSink.pages.size).toList(), koLargeSink.pages.map { it.third })

        // 2. LIG
        val ligTournament = com.example.data.model.Tournament(
            id = "t_lig",
            classRoomId = "c1",
            name = "Satranç Ligi",
            type = com.example.data.model.TournamentType.LIG,
            participants = students.take(4).map { it.id }
        )
        val ligMatches = com.example.util.TournamentEngine.generateRoundRobinFixture(
            tournamentId = ligTournament.id,
            participants = pairs8.take(4),
            isDoubleMatch = false
        )
        val ligSink = RecordingCanvasPdfSink()
        com.example.util.TournamentPdfHelper.exportTournamentPdfToFile(context, ligTournament, ligMatches, emptyList(), students, ligSink)
        assertTrue(ligSink.wroteBytes && ligSink.closed && ligSink.pages.isNotEmpty())

        // 3. GRUP
        val grpTournament = com.example.data.model.Tournament(
            id = "t_grp",
            classRoomId = "c1",
            name = "Mangala Grupları",
            type = com.example.data.model.TournamentType.GRUP,
            participants = students.take(8).map { it.id },
            groupCount = 2
        )
        val grpMatches = com.example.util.TournamentEngine.generateGroupFixtures(
            tournamentId = grpTournament.id,
            participants = pairs8,
            groupCount = 2,
            groupNaming = "letters",
            isDoubleMatch = false
        )
        val grpSink = RecordingCanvasPdfSink()
        com.example.util.TournamentPdfHelper.exportTournamentPdfToFile(context, grpTournament, grpMatches, emptyList(), students, grpSink)
        assertTrue(grpSink.wroteBytes && grpSink.closed && grpSink.pages.isNotEmpty())

        // 4. GRUP_ELEME (Projected playoff AND active knockout)
        val geTournament = grpTournament.copy(
            id = "t_ge",
            name = "Grup + Eleme Turnuvası",
            type = com.example.data.model.TournamentType.GRUP_ELEME,
            advancingPerGroup = 2
        )
        val geProjectedSink = RecordingCanvasPdfSink()
        com.example.util.TournamentPdfHelper.exportTournamentPdfToFile(context, geTournament, grpMatches, emptyList(), students, geProjectedSink)
        assertTrue(geProjectedSink.wroteBytes && geProjectedSink.closed)
        assertEquals((1..geProjectedSink.pages.size).toList(), geProjectedSink.pages.map { it.third })

        val geWithKoSink = RecordingCanvasPdfSink()
        com.example.util.TournamentPdfHelper.exportTournamentPdfToFile(
            context,
            geTournament,
            grpMatches + koMatches.take(2).map { it.copy(stage = "knockout", tournamentId = geTournament.id) },
            emptyList(),
            students,
            geWithKoSink
        )
        assertTrue(geWithKoSink.wroteBytes && geWithKoSink.closed)
        assertEquals((1..geWithKoSink.pages.size).toList(), geWithKoSink.pages.map { it.third })
    }

    @Test
    fun `test chat message serialization round trip and legacy backwards compatibility`() {
        val fullMsg = com.example.data.model.ChatMessage(
            id = "msg_full_1",
            classId = "class_4d",
            studentId = "std_1",
            studentNumber = "45",
            studentName = "Semih Ahmet",
            parentName = "Cihan Veli",
            parentEmail = "veli@example.com",
            isFromTeacher = true,
            senderName = "Cihan Öğretmen",
            text = "Yarınki gezi için izin belgesi lütfen imzalansın.",
            timestamp = 1_760_000_000_000L,
            status = com.example.data.model.MessageStatus.READ,
            replyToMessageId = "msg_prev",
            replyToSenderName = "Cihan Veli",
            replyToText = "Gezi saat kaçta başlıyor?",
            replyToIsFromTeacher = false,
            reactions = mapOf("teacher" to "👍", "parent_veli_example_com" to "❤️", "removed_key" to ""),
            deliveredAt = 1_760_000_005_000L,
            readAt = 1_760_000_020_000L,
            scheduledFor = 1_760_030_000_000L
        )

        val serialized = com.example.util.CommunicationPersistenceHelper.serializeChatMessageJson(fullMsg)
        val parsed = com.example.util.CommunicationPersistenceHelper.parseChatMessageJson(serialized)

        assertEquals(fullMsg.id, parsed.id)
        assertEquals(fullMsg.text, parsed.text)
        assertEquals("msg_prev", parsed.replyToMessageId)
        assertEquals("Cihan Veli", parsed.replyToSenderName)
        assertEquals("Gezi saat kaçta başlıyor?", parsed.replyToText)
        assertEquals(false, parsed.replyToIsFromTeacher)
        assertEquals(mapOf("teacher" to "👍", "parent_veli_example_com" to "❤️"), parsed.reactions)
        assertEquals(1_760_000_005_000L, parsed.deliveredAt)
        assertEquals(1_760_000_020_000L, parsed.readAt)
        assertEquals(1_760_030_000_000L, parsed.scheduledFor)

        // Legacy JSON without new fields must still parse cleanly
        val legacyJson = org.json.JSONObject().apply {
            put("id", "msg_legacy")
            put("classId", "class_4d")
            put("studentId", "std_1")
            put("studentNumber", "45")
            put("studentName", "Semih Ahmet")
            put("parentName", "Cihan Veli")
            put("isFromTeacher", false)
            put("senderName", "Cihan Veli")
            put("text", "Teşekkürler hocam")
            put("timestamp", 1_750_000_000_000L)
            put("status", "DELIVERED")
        }
        val legacyParsed = com.example.util.CommunicationPersistenceHelper.parseChatMessageJson(legacyJson)
        assertEquals("msg_legacy", legacyParsed.id)
        assertEquals(com.example.data.model.MessageStatus.DELIVERED, legacyParsed.status)
        assertEquals(null, legacyParsed.replyToMessageId)
        assertTrue(legacyParsed.reactions.isEmpty())
        assertEquals(null, legacyParsed.deliveredAt)
        assertEquals(null, legacyParsed.readAt)
        assertEquals(null, legacyParsed.scheduledFor)
    }

    @Test
    fun `test google auth helper uses single credential option per request and resolves web client id`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val clientId = com.example.auth.GoogleAuthHelper.getServerClientId(context)
        assertTrue("default_web_client_id must be present from google-services.json", clientId.isNotBlank())

        val interactiveRequest = com.example.auth.GoogleAuthHelper.buildInteractiveSignInRequest(clientId)
        assertEquals(
            "Interactive sign-in request must contain exactly 1 CredentialOption",
            1,
            interactiveRequest.credentialOptions.size
        )
        assertTrue(
            interactiveRequest.credentialOptions.first() is com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
        )

        val googleIdRequest = com.example.auth.GoogleAuthHelper.buildGoogleIdRequest(
            serverClientId = clientId,
            filterByAuthorizedAccounts = false,
            autoSelectEnabled = false
        )
        assertEquals(
            "Google ID request must contain exactly 1 CredentialOption",
            1,
            googleIdRequest.credentialOptions.size
        )
        assertTrue(
            googleIdRequest.credentialOptions.first() is com.google.android.libraries.identity.googleid.GetGoogleIdOption
        )
    }

    @Test
    fun `test web data migration zero loss integrity and end to end classroom persistence`() = kotlinx.coroutines.runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val jsonStr = com.example.util.WebDataMigrationHelper.readBundledMigrationJson(context)
        assertNotNull("Bundled migration JSON must be readable", jsonStr)
        assertTrue("Bundled migration JSON must not be blank", jsonStr!!.isNotBlank())

        val classId = "58_merkez_suleyman_sami_kepenek_ilkokulu_4d"
        val mapped = com.example.util.WebDataMigrationHelper.parseAndMapMigrationJson(
            jsonStr = jsonStr,
            classId = classId,
            teacherUid = "w0hA86kG5DSl7tI9kic46xVagbw1"
        )

        // 1. Students check (25 students)
        assertEquals(25, mapped.students.size)
        assertEquals("cihan.ozel10@gmail.com", mapped.teacherEmail)
        assertEquals("45", mapped.students.first().studentNumber)
        assertEquals("SEMİH AHMET KAVAKOĞLU", mapped.students.first().fullName)
        assertEquals("555", mapped.students.last().studentNumber)
        assertEquals("ARAS GÖKKAYA", mapped.students.last().fullName)

        // 2. Books check (113 books: 110 Rafta, 3 Okunuyor)
        assertEquals(113, mapped.books.size)
        assertEquals(3, mapped.activeLoanBooksCount)
        assertEquals(110, mapped.books.count { it.status == "Rafta" })
        assertEquals(3, mapped.books.count { it.status == "Okunuyor" })

        // Verify classic titles (#35 Don Kişot, #36 Pinokyo, #51 Keloğlan Masalları) are NOT treated as virtual/sample books
        val donKisot = mapped.books.firstOrNull { it.registrationNo == 35 }
        val pinokyo = mapped.books.firstOrNull { it.registrationNo == 36 }
        val keloglan = mapped.books.firstOrNull { it.registrationNo == 51 }
        assertNotNull(donKisot)
        assertNotNull(pinokyo)
        assertNotNull(keloglan)
        org.junit.Assert.assertFalse(com.example.util.LibraryPersistenceHelper.isVirtualOrCorruptBook(donKisot!!))
        org.junit.Assert.assertFalse(com.example.util.LibraryPersistenceHelper.isVirtualOrCorruptBook(pinokyo!!))
        org.junit.Assert.assertFalse(com.example.util.LibraryPersistenceHelper.isVirtualOrCorruptBook(keloglan!!))

        // Verify active book loans (#79 -> #45, #100 -> #407, #112 -> #405)
        val book79 = mapped.books.first { it.registrationNo == 79 }
        val book100 = mapped.books.first { it.registrationNo == 100 }
        val book112 = mapped.books.first { it.registrationNo == 112 }
        assertEquals("std_45", book79.currentStudentId)
        assertEquals("SEMİH AHMET KAVAKOĞLU", book79.currentStudentName)
        assertEquals("std_407", book100.currentStudentId)
        assertEquals("EMİR ASAF AKKAŞ", book100.currentStudentName)
        assertEquals("std_405", book112.currentStudentId)
        assertEquals("BAHATTİN SÜER", book112.currentStudentName)

        // 3. Reading records check (214 records)
        assertEquals(214, mapped.readingRecords.size)

        // 4. Star records check (1,325 records totaling 10,084 stars)
        assertEquals(1325, mapped.starRecords.size)
        assertEquals(10084, mapped.totalStarsSum)

        // 5. Per-student 100% reconciliation against verificationSummary
        assertEquals(25, mapped.verificationItems.size)
        for (vItem in mapped.verificationItems) {
            val student = mapped.students.first { it.studentNumber == vItem.studentNo.toString() }
            val studentStars = mapped.starRecords.filter { it.studentId == student.id }.sumOf { it.starCount }
            val studentReads = mapped.readingRecords.count { it.studentId == student.id }
            assertEquals("Star sum mismatch for #${vItem.studentNo}", vItem.siteTotalStarsShownToUser, studentStars)
            assertEquals("Reading count mismatch for #${vItem.studentNo}", vItem.readingCount, studentReads)
        }

        // 6. Preserve existing linked parents & custom Firestore student IDs
        val existingCustomStudent = com.example.data.model.Student(
            id = "custom_firestore_doc_426",
            studentNumber = "426",
            fullName = "ELİF YAZICI",
            gender = "Kız",
            parentInviteCode = "CUSTOM88",
            parentName = "Ayşe Yazıcı",
            parentEmail = "ayse.yazici@example.com",
            linkedParents = listOf(
                com.example.data.model.LinkedParentAccount(
                    email = "ayse.yazici@example.com",
                    name = "Ayşe Yazıcı",
                    relationship = "Anne",
                    linkedAt = "15 Kasım 2026"
                )
            )
        )
        val mergedWithExisting = com.example.util.WebDataMigrationHelper.parseAndMapMigrationJson(
            jsonStr = jsonStr,
            classId = classId,
            teacherUid = "teacher_1",
            existingStudents = listOf(existingCustomStudent)
        )
        val elif = mergedWithExisting.students.first { it.studentNumber == "426" }
        assertEquals("custom_firestore_doc_426", elif.id)
        assertEquals("CUSTOM88", elif.parentInviteCode)
        assertTrue(elif.isParentLinked)
        assertEquals(540, mergedWithExisting.starRecords.filter { it.studentId == "custom_firestore_doc_426" }.sumOf { it.starCount })
        assertEquals(11, mergedWithExisting.readingRecords.count { it.studentId == "custom_firestore_doc_426" })

        // 7. End-to-end migrateClassroomData persistence + cleanup safety check
        val repo = com.example.data.repository.ClassRegistryRepository(context)
        repo.enableTestMode("test_uid")
        val summary = com.example.util.WebDataMigrationHelper.migrateClassroomData(
            context = context,
            repository = repo,
            classId = classId,
            teacherUid = "test_uid",
            force = true
        )
        assertTrue(summary.success)
        assertEquals(25, summary.studentsCount)
        assertEquals(113, summary.booksCount)
        assertEquals(110, summary.booksOnShelfCount)
        assertEquals(3, summary.activeLoanBooksCount)
        assertEquals(214, summary.readingRecordsCount)
        assertEquals(1325, summary.starRecordsCount)
        assertEquals(10084, summary.totalStarsCount)

        // Verify cleanupStaleAndOrphanedLibraryData and cleanupStaleAndOrphanedStarRecords do NOT delete any migrated records!
        val cleanupReport = com.example.util.LibraryPersistenceHelper.cleanupStaleAndOrphanedLibraryData(
            context = context,
            classId = classId,
            validStudents = repo.students.value,
            resequenceNumbers = false
        )
        assertEquals(0, cleanupReport.removedBooksCount)
        assertEquals(0, cleanupReport.removedRecordsCount)
        assertEquals(0, cleanupReport.revokedStarsCount)

        val loadedBooksAfterCleanup = com.example.util.LibraryPersistenceHelper.loadBooks(context, classId)
        val loadedRecordsAfterCleanup = com.example.util.LibraryPersistenceHelper.loadReadingRecords(context, classId)
        val loadedStarsAfterCleanup = com.example.data.repository.StarRewardPersistenceHelper.loadStarRecords(context, classId)
        assertEquals(113, loadedBooksAfterCleanup.size)
        assertEquals(214, loadedRecordsAfterCleanup.size)
        assertEquals(1325, loadedStarsAfterCleanup.size)
        assertEquals(10084, loadedStarsAfterCleanup.sumOf { it.starCount })
    }

    @Test
    fun `test firestore auth rest parsers and per uid account session bookmarks`() {
        val restHelper = com.example.util.FirestoreAuthRestHelper
        val routingHelper = com.example.util.AuthSessionRoutingHelper

        // 1. Parse teacher UserProfile from Firestore REST v1 document JSON
        val teacherProfileJson = """
            {
              "name": "projects/biriz-app/databases/ai-studio-db/documents/users/uid_teacher_4d",
              "fields": {
                "userId": {"stringValue": "uid_teacher_4d"},
                "fullName": {"stringValue": "Cihan Özel"},
                "email": {"stringValue": "cihan.ozel@okul.edu.tr"},
                "role": {"stringValue": "TEACHER"},
                "kvkkAccepted": {"booleanValue": true},
                "assignedClassId": {"stringValue": "58_merkez_ssk_4d"},
                "assignedSchoolId": {"stringValue": "58_merkez_ssk"}
              }
            }
        """.trimIndent()
        val parsedTeacher = restHelper.parseUserProfileFromRestJson(
            uid = "uid_teacher_4d",
            fallbackName = "Fallback",
            fallbackEmail = "fallback@example.com",
            rawJson = teacherProfileJson
        )
        assertNotNull(parsedTeacher)
        assertEquals("uid_teacher_4d", parsedTeacher!!.userId)
        assertEquals("TEACHER", parsedTeacher.role)
        assertEquals("58_merkez_ssk_4d", parsedTeacher.assignedClassId)
        assertTrue(parsedTeacher.kvkkAccepted)
        assertTrue(parsedTeacher.loadedFromCloud)
        org.junit.Assert.assertFalse(parsedTeacher.isFreshRegistration)

        // 2. Parse parent UserProfile with linkedStudentIds array
        val parentProfileJson = """
            {
              "name": "projects/biriz-app/databases/ai-studio-db/documents/users/uid_parent_1",
              "fields": {
                "userId": {"stringValue": "uid_parent_1"},
                "fullName": {"stringValue": "Ayşe Yılmaz"},
                "email": {"stringValue": "ayse@example.com"},
                "role": {"stringValue": "PARENT"},
                "linkedClassroomId": {"stringValue": "58_merkez_ssk_4d"},
                "linkedStudentIds": {
                  "arrayValue": {
                    "values": [
                      {"stringValue": "std_45"},
                      {"stringValue": "std_46"}
                    ]
                  }
                }
              }
            }
        """.trimIndent()
        val parsedParent = restHelper.parseUserProfileFromRestJson(
            uid = "uid_parent_1",
            fallbackName = "Fallback",
            fallbackEmail = "ayse@example.com",
            rawJson = parentProfileJson
        )
        assertNotNull(parsedParent)
        assertEquals("PARENT", parsedParent!!.role)
        assertEquals("58_merkez_ssk_4d", parsedParent.linkedClassroomId)
        assertEquals(listOf("std_45", "std_46"), parsedParent.linkedStudentIds)
        assertTrue(parsedParent.loadedFromCloud)

        // 3. Parse runQuery response for ClassRoom and Student path extraction
        val runQueryJson = """
            [
              {
                "document": {
                  "name": "projects/biriz-app/databases/ai-studio-db/documents/classrooms/58_merkez_ssk_4d",
                  "fields": {
                    "cityName": {"stringValue": "Sivas"},
                    "districtName": {"stringValue": "Merkez"},
                    "schoolName": {"stringValue": "SÜLEYMAN SAMİ KEPENEK İLKOKULU"},
                    "gradeLevel": {"integerValue": "4"},
                    "section": {"stringValue": "D"},
                    "teacherId": {"stringValue": "uid_teacher_4d"},
                    "teacherName": {"stringValue": "Cihan Özel"},
                    "teacherEmail": {"stringValue": "cihan.ozel@okul.edu.tr"},
                    "inviteCode": {"stringValue": "ABC123"}
                  }
                }
              },
              {
                "readTime": "2026-11-28T06:00:00Z"
              }
            ]
        """.trimIndent()
        val queryDocs = restHelper.parseRunQueryDocuments(runQueryJson)
        assertEquals(1, queryDocs.size)
        val parsedRoom = restHelper.parseClassRoomFromRestDoc(queryDocs.first())
        assertNotNull(parsedRoom)
        assertEquals("58_merkez_ssk_4d", parsedRoom!!.id)
        assertEquals(4, parsedRoom.gradeLevel)
        assertEquals("D", parsedRoom.section)
        assertEquals("uid_teacher_4d", parsedRoom.teacherId)

        val extractedClassId = restHelper.extractParentClassroomIdFromStudentPath(
            "projects/biriz-app/databases/ai-studio-db/documents/classrooms/58_merkez_ssk_4d/students/std_45"
        )
        assertEquals("58_merkez_ssk_4d", extractedClassId)

        val queryPayload = restHelper.buildEqualityQueryPayload(
            collectionId = "students",
            fieldPath = "parentEmail",
            stringValue = "ayse@example.com",
            limit = 10,
            allDescendants = true
        )
        assertTrue(queryPayload.contains("\"allDescendants\":true"))
        assertTrue(queryPayload.contains("\"fieldPath\":\"parentEmail\""))

        // 4. Per-UID AccountSessionBookmark upsert, lookup, cross-user isolation, and purge on delete
        val teacherBookmark = com.example.util.AuthSessionRoutingHelper.AccountSessionBookmark(
            uid = "uid_teacher_4d",
            email = "cihan.ozel@okul.edu.tr",
            roleCode = "OGRETMEN",
            classId = "58_merkez_ssk_4d",
            schoolName = "SÜLEYMAN SAMİ KEPENEK İLKOKULU",
            gradeLevel = 4,
            section = "D",
            updatedAtMs = 1_000L
        )
        val parentBookmark = com.example.util.AuthSessionRoutingHelper.AccountSessionBookmark(
            uid = "uid_parent_1",
            email = "ayse@example.com",
            roleCode = "VELI",
            classId = "58_merkez_ssk_4d",
            linkedStudentIds = listOf("std_45", "std_46"),
            updatedAtMs = 2_000L
        )
        val json1 = routingHelper.upsertBookmarkJson("", teacherBookmark)
        val json2 = routingHelper.upsertBookmarkJson(json1, parentBookmark)

        val foundTeacher = routingHelper.findBookmarkFromJson(json2, "uid_teacher_4d", "cihan.ozel@okul.edu.tr")
        assertNotNull(foundTeacher)
        assertEquals("OGRETMEN", foundTeacher!!.roleCode)
        assertEquals("58_merkez_ssk_4d", foundTeacher.classId)

        val foundParent = routingHelper.findBookmarkFromJson(json2, "uid_parent_1", "ayse@example.com")
        assertNotNull(foundParent)
        assertEquals("VELI", foundParent!!.roleCode)
        assertEquals(listOf("std_45", "std_46"), foundParent.linkedStudentIds)

        // Different UID with same email MUST NOT steal another UID's bookmark
        val wrongUidMatch = routingHelper.findBookmarkFromJson(json2, "uid_different_999", "ayse@example.com")
        org.junit.Assert.assertNull(wrongUidMatch)

        // Removing parent bookmark deletes only parent, preserving teacher bookmark
        val afterRemove = routingHelper.removeBookmarkFromJson(json2, "uid_parent_1", "ayse@example.com")
        org.junit.Assert.assertNull(routingHelper.findBookmarkFromJson(afterRemove, "uid_parent_1", "ayse@example.com"))
        assertNotNull(routingHelper.findBookmarkFromJson(afterRemove, "uid_teacher_4d", "cihan.ozel@okul.edu.tr"))
    }
}


