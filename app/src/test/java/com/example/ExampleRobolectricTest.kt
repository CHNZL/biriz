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
}

