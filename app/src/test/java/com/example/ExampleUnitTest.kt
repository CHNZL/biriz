package com.example

import org.junit.Assert.*
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testVeliKatilimKoduGenerationRules() {
    val prohibitedChars = setOf('Ç', 'Ğ', 'İ', 'Ö', 'Ş', 'Ü', 'ç', 'ğ', 'ı', 'i', 'ö', 'ş', 'ü', 'I', '1', 'O', '0', 'S', '5')

    for (i in 1..200) {
      val code = com.example.util.TurkishTextHelper.generateVeliKatilimKodu()
      assertEquals("Code must be exactly 8 characters", 8, code.length)
      for (char in code) {
        assertFalse(
          "Code contains prohibited or confusing char '$char' in '$code'",
          prohibitedChars.contains(char)
        )
        assertTrue(
          "Code must only contain uppercase letters and digits: '$char'",
          char in 'A'..'Z' || char in '0'..'9'
        )
      }
    }
  }

  @Test
  fun testTurkishUpperCaseConversion() {
    assertEquals("VELİ ŞAHİN", com.example.util.TurkishTextHelper.toTurkishUpperCase("veli şahin"))
    assertEquals("ÖMER ÇELİK", com.example.util.TurkishTextHelper.toTurkishUpperCase("ömer çelik"))
    assertEquals("IŞIL GÜNER", com.example.util.TurkishTextHelper.toTurkishUpperCase("ışıl güner"))
    assertEquals("AHMET YILMAZ", com.example.util.TurkishTextHelper.toTurkishUpperCase("ahmet yılmaz"))
  }

  @Test
  fun testScheduleTimeCalculations() {
    val helper = com.example.util.ScheduleHelper
    assertEquals("09:10", helper.addMinutes("08:30", 40))
    assertEquals("09:25", helper.addMinutes("09:10", 15))
    assertEquals("10:05", helper.addMinutes("09:25", 40))
    assertEquals("12:40", helper.addMinutes("11:50", 50))
  }

  @Test
  fun testScheduleTimelineMatchesImageSpec() {
    val config = com.example.data.model.ScheduleConfig(
      dailyLessonCount = 6,
      firstLessonStartTime = "08:30",
      lessonDurationMinutes = 40,
      recessDurations = listOf(15, 15, 10, 15, 15),
      isLunchBreakActive = true,
      lunchAfterLessonIndex = 4,
      lunchDurationMinutes = 50
    )

    val defaultLessons = com.example.util.ScheduleHelper.generateDefaultWeeklySchedule()
    val mondayLessons = defaultLessons.filter { it.dayOfWeek == "PZT" }
    val timeline = com.example.util.ScheduleHelper.calculateTimeline(config, mondayLessons)

    // Check 1. Lesson: 08:30 - 09:10
    val l1 = timeline[0] as com.example.data.model.ScheduleTimelineItem.LessonSlot
    assertEquals(1, l1.lessonIndex)
    assertEquals("08:30", l1.startTime)
    assertEquals("09:10", l1.endTime)
    assertEquals("TÜRKÇE", l1.lesson?.subjectName)

    // Check 1. Recess: 09:10 - 09:25 (15 dk)
    val r1 = timeline[1] as com.example.data.model.ScheduleTimelineItem.RecessSlot
    assertEquals("09:10", r1.startTime)
    assertEquals("09:25", r1.endTime)
    assertEquals(15, r1.durationMinutes)

    // Check 2. Lesson: 09:25 - 10:05 (Matematik)
    val l2 = timeline[2] as com.example.data.model.ScheduleTimelineItem.LessonSlot
    assertEquals(2, l2.lessonIndex)
    assertEquals("09:25", l2.startTime)
    assertEquals("10:05", l2.endTime)
    assertEquals("MATEMATİK", l2.lesson?.subjectName)

    // Check 3. Recess: 11:00 - 11:10 (10 dk - custom flexible recess)
    val r3 = timeline[5] as com.example.data.model.ScheduleTimelineItem.RecessSlot
    assertEquals("11:00", r3.startTime)
    assertEquals("11:10", r3.endTime)
    assertEquals(10, r3.durationMinutes)

    // Check 4. Lesson: 11:10 - 11:50 (Sosyal Bilgiler)
    val l4 = timeline[6] as com.example.data.model.ScheduleTimelineItem.LessonSlot
    assertEquals(4, l4.lessonIndex)
    assertEquals("11:10", l4.startTime)
    assertEquals("11:50", l4.endTime)

    // Check Lunch Break: 11:50 - 12:40 (50 dk)
    val lunch = timeline[7] as com.example.data.model.ScheduleTimelineItem.LunchBreakSlot
    assertEquals(4, lunch.afterLessonIndex)
    assertEquals("11:50", lunch.startTime)
    assertEquals("12:40", lunch.endTime)
    assertEquals(50, lunch.durationMinutes)

    // Check 5. Lesson: 12:40 - 13:20 (Müzik)
    val l5 = timeline[8] as com.example.data.model.ScheduleTimelineItem.LessonSlot
    assertEquals(5, l5.lessonIndex)
    assertEquals("12:40", l5.startTime)
    assertEquals("13:20", l5.endTime)
    assertEquals("MÜZİK", l5.lesson?.subjectName)
  }

  @Test
  fun testTwentyLessonColorsPalette() {
    val palette = com.example.util.ScheduleHelper.LESSON_COLOR_PALETTE
    assertEquals("Must have exactly 20 colors in palette", 20, palette.size)

    val keys = palette.map { it.key }
    assertEquals("All keys must be unique", 20, keys.distinct().size)

    // Check that getColorsForTheme returns valid pairs
    val navy = com.example.util.ScheduleHelper.getColorsForTheme("NAVY")
    assertEquals("#13294B", navy.first)
    assertEquals("#1E3A8A", navy.second)

    val coral = com.example.util.ScheduleHelper.getColorsForTheme("CORAL")
    assertEquals("#BE123C", coral.first)
    assertEquals("#F43F5E", coral.second)
  }

  @Test
  fun testIdenticalLessonColorSynchronization() {
    val lessons = mutableListOf(
      com.example.data.model.ScheduleLesson("PZT", 1, "VATANDAŞLIK", "", "", "NAVY"),
      com.example.data.model.ScheduleLesson("SAL", 3, "vatandaşlık", "", "", "NAVY"),
      com.example.data.model.ScheduleLesson("ÇAR", 2, "MATEMATİK", "", "", "AMBER")
    )

    // When teacher changes one "vatandaşlık" or "VATANDAŞLIK" to "EMERALD"
    val updatedSubject = com.example.util.TurkishTextHelper.toTurkishUpperCase("vatandaşlık")
    assertEquals("VATANDAŞLIK", updatedSubject)

    val newColor = "EMERALD"
    for (i in 0 until lessons.size) {
      if (com.example.util.TurkishTextHelper.toTurkishUpperCase(lessons[i].subjectName.trim()) == updatedSubject) {
        lessons[i] = lessons[i].copy(
          subjectName = updatedSubject,
          colorTheme = newColor
        )
      }
    }

    val vatLessons = lessons.filter { it.subjectName == "VATANDAŞLIK" }
    assertEquals(2, vatLessons.size)
    assertTrue("All VATANDAŞLIK lessons must have EMERALD color", vatLessons.all { it.colorTheme == "EMERALD" })
    assertEquals("MATEMATİK must remain untouched", "AMBER", lessons.first { it.subjectName == "MATEMATİK" }.colorTheme)
  }

  @Test
  fun testLunchDurationOptionsFrom20To90MinutesIn5MinuteSteps() {
    val options = (20..90 step 5).toList()
    assertEquals(15, options.size)
    assertEquals(20, options.first())
    assertEquals(90, options.last())
    assertEquals(listOf(20, 25, 30, 35, 40, 45, 50, 55, 60, 65, 70, 75, 80, 85, 90), options)
  }

  @Test
  fun testSeatingPlanSeatIdParsingAndGeneration() {
    val helper = com.example.util.SeatingPlanHelper
    val parsed = helper.parseSeatId("g1-r3-s0")
    assertNotNull(parsed)
    assertEquals(1, parsed!!.groupIndex)
    assertEquals(3, parsed.rowIndex)
    assertEquals(0, parsed.seatIndex)

    assertNull(helper.parseSeatId("invalid-seat-id"))

    val config = com.example.data.model.SeatingConfig(
      groupCount = 2,
      peoplePerRow = 2,
      rowsPerGroup = listOf(3, 2)
    )
    val frontToBackSeats = helper.generateAllSeatIdsFrontToBack(config)
    // Row 0 first across groups: g0-r0-s0, g0-r0-s1, g1-r0-s0, g1-r0-s1
    assertEquals("g0-r0-s0", frontToBackSeats[0])
    assertEquals("g0-r0-s1", frontToBackSeats[1])
    assertEquals("g1-r0-s0", frontToBackSeats[2])
    assertEquals("g1-r0-s1", frontToBackSeats[3])

    // Total seats: (3 * 2) + (2 * 2) = 10
    assertEquals(10, frontToBackSeats.size)
    assertEquals(10, config.totalSeats)
  }

  @Test
  fun testSeatingPlanAutoAssignPriorityAndPinnedPreservation() {
    val helper = com.example.util.SeatingPlanHelper
    val students = listOf(
      com.example.data.model.Student(id = "s1", studentNumber = "101", fullName = "Zeynep Kaya", gender = "Kız", parentInviteCode = "K9M2P7XZ"),
      com.example.data.model.Student(id = "s2", studentNumber = "102", fullName = "Ahmet Yılmaz", gender = "Erkek", parentInviteCode = "A8B3C4D5"),
      com.example.data.model.Student(id = "s3", studentNumber = "103", fullName = "Elif Demir", gender = "Kız", parentInviteCode = "E7F6G5H4"),
      com.example.data.model.Student(id = "s4", studentNumber = "104", fullName = "Can Aksoy", gender = "Erkek", parentInviteCode = "J3K2L1M9")
    )

    val config = com.example.data.model.SeatingConfig(
      groupCount = 2,
      peoplePerRow = 2,
      rowsPerGroup = listOf(2, 2)
    )

    // Pin student s2 to back seat "g0-r1-s0"
    // Set student s4 with glasses/attention priority (👓)
    val initialState = com.example.data.model.SeatingPlanState(
      config = config,
      assignments = mapOf("g0-r1-s0" to "s2"),
      pinnedStudents = setOf("s2"),
      priorityStudents = setOf("s4")
    )

    val assignedState = helper.autoAssignSeats(
      students = students,
      currentState = initialState,
      mode = com.example.data.model.SeatingPlacementMode.PRIORITY_FRONT
    )

    // Pinned student s2 MUST remain at "g0-r1-s0"
    assertEquals("s2", assignedState.assignments["g0-r1-s0"])

    // Priority student s4 MUST be seated in row 0 (front row: g0-r0-s0, g0-r0-s1, g1-r0-s0, or g1-r0-s1)
    val s4Seat = assignedState.assignments.entries.first { it.value == "s4" }.key
    val s4Info = helper.parseSeatId(s4Seat)!!
    assertEquals("Priority student s4 must be placed in row 0", 0, s4Info.rowIndex)

    // Swap test
    val swappedAssignments = helper.swapSeats(assignedState.assignments, "g0-r0-s0", "g1-r0-s0")
    assertEquals(assignedState.assignments["g0-r0-s0"], swappedAssignments["g1-r0-s0"])
    assertEquals(assignedState.assignments["g1-r0-s0"], swappedAssignments["g0-r0-s0"])
  }

  @Test
  fun testGroupMakerMathModes() {
    val helper = com.example.util.GroupMakerHelper
    val configCount = com.example.data.model.GroupDistributionConfig(
      mode = com.example.data.model.GroupDistributionMode.BY_GROUP_COUNT,
      targetGroupCount = 4
    )
    assertEquals(4, helper.calculateGroupCount(24, configCount))

    val configPerGroup = com.example.data.model.GroupDistributionConfig(
      mode = com.example.data.model.GroupDistributionMode.BY_STUDENTS_PER_GROUP,
      targetStudentsPerGroup = 5
    )
    // 24 students / 5 = ceil(4.8) = 5 groups
    assertEquals(5, helper.calculateGroupCount(24, configPerGroup))
  }

  @Test
  fun testGroupMakerGenderBalancingAlgorithm() {
    val helper = com.example.util.GroupMakerHelper
    val students = mutableListOf<com.example.data.model.Student>()
    // 6 Erkek, 6 Kız -> Toplam 12 öğrenci
    for (i in 1..6) {
      students.add(com.example.data.model.Student(id = "b$i", studentNumber = "10$i", fullName = "Erkek $i", gender = "ERKEK"))
      students.add(com.example.data.model.Student(id = "g$i", studentNumber = "20$i", fullName = "Kız $i", gender = "KIZ"))
    }

    val config = com.example.data.model.GroupDistributionConfig(
      mode = com.example.data.model.GroupDistributionMode.BY_GROUP_COUNT,
      targetGroupCount = 3,
      balanceGender = true
    )

    val (groups, unassigned) = helper.distributeStudents(students, config)
    assertEquals(3, groups.size)
    assertTrue("No students should remain unassigned", unassigned.isEmpty())

    // Her grupta tam olarak 4 öğrenci (2 Erkek, 2 Kız) olmalıdır!
    groups.forEach { group ->
      assertEquals(4, group.studentIds.size)
      val members = students.filter { group.studentIds.contains(it.id) }
      val boys = members.count { it.gender == "ERKEK" }
      val girls = members.count { it.gender == "KIZ" }
      assertEquals("Each group must have exactly 2 boys", 2, boys)
      assertEquals("Each group must have exactly 2 girls", 2, girls)
    }
  }

  @Test
  fun testGroupMakerSwapMoveAndLeader() {
    val helper = com.example.util.GroupMakerHelper
    val group1 = com.example.data.model.GroupTeam(id = "g1", name = "Kartallar", studentIds = listOf("s1", "s2"), leaderStudentId = "s1")
    val group2 = com.example.data.model.GroupTeam(id = "g2", name = "Yıldızlar", studentIds = listOf("s3", "s4"))

    val initialGroups = listOf(group1, group2)

    // Swap s1 and s3
    val swapped = helper.swapStudents(initialGroups, "s1", "s3")
    val updatedG1 = swapped.first { it.id == "g1" }
    val updatedG2 = swapped.first { it.id == "g2" }

    assertTrue("g1 must contain s3", updatedG1.studentIds.contains("s3"))
    assertTrue("g2 must contain s1", updatedG2.studentIds.contains("s1"))
    assertEquals("Leader of g1 should now be s3", "s3", updatedG1.leaderStudentId)

    // Move s4 from g2 to g1
    val (movedGroups, unassigned) = helper.moveStudentToGroup(swapped, emptyList(), "s4", "g1")
    assertEquals(3, movedGroups.first { it.id == "g1" }.studentIds.size)
    assertEquals(1, movedGroups.first { it.id == "g2" }.studentIds.size)
    assertTrue(unassigned.isEmpty())
  }

  @Test
  fun testStandardShareTexts() {
    val classRoom = com.example.data.model.ClassRoom(
      id = "c1",
      cityName = "Ankara",
      districtName = "Çankaya",
      schoolName = "Atatürk İlkokulu",
      gradeLevel = 3,
      section = "A",
      teacherName = "Ahmet Öğretmen"
    )
    val students = listOf(
      com.example.data.model.Student(id = "s1", studentNumber = "101", fullName = "Ali Kaya", gender = "Erkek"),
      com.example.data.model.Student(id = "s2", studentNumber = "102", fullName = "Ayşe Demir", gender = "Kız")
    )

    // Class list share text
    val classListText = com.example.util.ExcelPdfHelper.generateClassListShareText(classRoom, students)
    assertTrue(classListText.contains("Atatürk İlkokulu"))
    assertTrue(classListText.contains("Ali Kaya"))
    assertTrue(classListText.contains("Ayşe Demir"))
    assertTrue(classListText.contains("Toplam: 2 Öğrenci"))

    // Schedule share text
    val config = com.example.data.model.ScheduleConfig(dailyLessonCount = 2, firstLessonStartTime = "09:00", lessonDurationMinutes = 40)
    val lessons = listOf(
      com.example.data.model.ScheduleLesson(dayOfWeek = "PZT", lessonIndex = 1, subjectName = "Türkçe"),
      com.example.data.model.ScheduleLesson(dayOfWeek = "PZT", lessonIndex = 2, subjectName = "Matematik")
    )
    val scheduleText = com.example.util.ExcelPdfHelper.generateWeeklyScheduleShareText(classRoom, config, lessons)
    assertTrue(scheduleText.contains("Haftalık Ders Programı"))
    assertTrue(scheduleText.contains("PAZARTESİ"))
    assertTrue(scheduleText.contains("Türkçe"))
    assertTrue(scheduleText.contains("Matematik"))

    // Seating plan share text
    val seatingState = com.example.data.model.SeatingPlanState(
      config = com.example.data.model.SeatingConfig(groupCount = 2, peoplePerRow = 2, rowsPerGroup = listOf(2, 2)),
      assignments = mapOf("g0-r0-s0" to "s1", "g0-r0-s1" to "s2")
    )
    val seatingText = com.example.util.ExcelPdfHelper.generateSeatingPlanShareText(classRoom, seatingState, students)
    assertTrue(seatingText.contains("Sınıf Oturma Planı"))
    assertTrue(seatingText.contains("Ali Kaya"))
    assertTrue(seatingText.contains("Ayşe Demir"))
  }

  @Test
  fun testTeacherRoomDocumentShareTexts() {
    val classRoom = com.example.data.model.ClassRoom(
      id = "c1",
      cityName = "Ankara",
      districtName = "Çankaya",
      schoolName = "Atatürk İlkokulu",
      gradeLevel = 3,
      section = "A",
      teacherName = "Ahmet Öğretmen"
    )
    val students = listOf(
      com.example.data.model.Student(id = "s1", studentNumber = "101", fullName = "Ali Kaya", gender = "Erkek"),
      com.example.data.model.Student(id = "s2", studentNumber = "102", fullName = "Ayşe Demir", gender = "Kız")
    )

    // 1. Zümre Toplantısı
    val zumreState = com.example.util.ZumreToplantiState(
      academicYear = "2026-2027",
      semester = "1. Dönem Başı",
      subjectName = "Sınıf Öğretmenleri Zümresi"
    )
    val zumreText = com.example.util.TeacherDocumentHelper.generateZumreShareText(classRoom, zumreState)
    assertTrue(zumreText.contains("Atatürk İlkokulu"))
    assertTrue(zumreText.contains("2026-2027"))
    assertTrue(zumreText.contains("GÜNDEM MADDELERİ"))
    assertTrue(zumreText.contains("ALINAN KARARLAR"))

    // 2. Veli Toplantısı
    val veliState = com.example.util.VeliToplantiState(
      termName = "1. Dönem 1. Olağan Veli Toplantısı"
    )
    val veliText = com.example.util.TeacherDocumentHelper.generateVeliToplantiShareText(classRoom, veliState, students)
    assertTrue(veliText.contains("3/A"))
    assertTrue(veliText.contains("TOPLANTI GÜNDEMİ"))
    assertTrue(veliText.contains("Kayıtlı Öğrenci Sayısı: 2"))

    // 3. Sosyal Kulüp
    val kulupState = com.example.util.SosyalKulupState(
      clubName = "Kütüphanecilik ve Okuma Kulübü",
      advisorTeacher = "Ahmet Öğretmen"
    )
    val kulupText = com.example.util.TeacherDocumentHelper.generateSosyalKulupShareText(classRoom, kulupState)
    assertTrue(kulupText.contains("KÜTÜPHANECİLİK VE OKUMA KULÜBÜ"))
    assertTrue(kulupText.contains("Danışman Öğretmen: Ahmet Öğretmen"))
    assertTrue(kulupText.contains("YILLIK ÇALIŞMA TAKVİMİ"))

    // 4. Okul Gezisi
    val geziState = com.example.util.OkulGeziState(
      destination = "Anıtkabir Gezisi",
      purpose = "Millî Tarih Bilinci"
    )
    val geziText = com.example.util.TeacherDocumentHelper.generateOkulGeziShareText(classRoom, geziState, 25)
    assertTrue(geziText.contains("Anıtkabir Gezisi"))
    assertTrue(geziText.contains("Katılacak Öğrenci Sayısı: 25"))
    assertTrue(geziText.contains("veli muvafakat belgesini"))

    // 5. Dilekçe
    val dilekceState = com.example.util.DilekceState(
      subject = "Mazeret İzni"
    )
    val dilekceText = com.example.util.TeacherDocumentHelper.generateDilekceShareText(classRoom, dilekceState)
    assertTrue(dilekceText.contains("ATATÜRK İLKOKULU MÜDÜRLÜĞÜNE"))
    assertTrue(dilekceText.contains("Ahmet Öğretmen"))
    assertTrue(dilekceText.contains("Mazeret İzni"))
  }

  @Test
  fun testStarRewardCategoriesAndCalculation() {
    val categories = com.example.data.model.StarCategory.ALL_CATEGORIES
    assertEquals(17, categories.size)

    val academic = categories.filter { it.isAcademic }
    assertEquals(9, academic.size)

    val values = categories.filter { !it.isAcademic }
    assertEquals(8, values.size)

    val records = listOf(
      com.example.data.model.StarRewardRecord(
        id = "1",
        classId = "class_1",
        studentId = "std_1",
        studentName = "Ali Veli",
        studentNumber = "101",
        categoryId = "matematik",
        categoryName = "Matematik",
        starCount = 5,
        note = "Problem çözümü"
      ),
      com.example.data.model.StarRewardRecord(
        id = "2",
        classId = "class_1",
        studentId = "std_1",
        studentName = "Ali Veli",
        studentNumber = "101",
        categoryId = "matematik",
        categoryName = "Matematik",
        starCount = 5,
        note = "Problem çözümü (Şanslı Öğrenci Ödülü 👑)",
        isLuckyBonus = true
      ),
      com.example.data.model.StarRewardRecord(
        id = "3",
        classId = "class_1",
        studentId = "std_2",
        studentName = "Ayşe Fatma",
        studentNumber = "102",
        categoryId = "turkce",
        categoryName = "Türkçe",
        starCount = 3,
        note = "Şiir okuma"
      )
    )

    assertEquals(10, com.example.data.repository.StarRewardPersistenceHelper.getStudentTotalStars(records, "std_1"))
    assertEquals(3, com.example.data.repository.StarRewardPersistenceHelper.getStudentTotalStars(records, "std_2"))
    assertEquals(0, com.example.data.repository.StarRewardPersistenceHelper.getStudentTotalStars(records, "std_3"))
  }

  @Test
  fun testAppUpdateNotificationConfiguration() {
    assertEquals("biriz_update_notifications", com.example.util.AppUpdateNotificationHelper.CHANNEL_ID)
    assertEquals(2026, com.example.util.AppUpdateNotificationHelper.NOTIFICATION_ID)
    assertEquals("extra_open_update_dialog", com.example.util.AppUpdateNotificationHelper.EXTRA_OPEN_UPDATE_DIALOG)

    val updateInfo = com.example.util.UpdateInfo(
      isUpdateAvailable = true,
      currentVersionName = "1.0.0",
      currentVersionCode = 1000,
      latestVersionName = "1.0.1",
      latestVersionCode = 1001,
      downloadUrl = "https://github.com/test/repo/releases/download/v1.0.1/Biriz.apk",
      releaseNotes = "Hata düzeltmeleri ve yeni bildirim özellikleri."
    )
    assertTrue("Update must be marked available", updateInfo.isUpdateAvailable)
    assertEquals("1.0.1", updateInfo.latestVersionName)
    assertTrue("Download URL must point to apk", updateInfo.downloadUrl.endsWith(".apk"))
  }

  @Test
  fun testNotificationTypeAppUpdateAndAutoDismissLogic() {
    val notif = com.example.data.model.BirizNotification(
      id = "update_1001",
      title = "🚀 Yeni Güncelleme Yayında! (v1.0.1)",
      message = "Yüklemek için dokunun.",
      type = com.example.data.model.NotificationType.APP_UPDATE,
      targetSubPageName = "APP_UPDATE"
    )
    assertEquals(com.example.data.model.NotificationType.APP_UPDATE, notif.type)

    val notifications = listOf(
      notif,
      com.example.data.model.BirizNotification("msg_1", "Mesaj", "Veli mesajı", type = com.example.data.model.NotificationType.MESSAGE, targetSubPageName = "ILETISIM_MESAJLAR")
    )

    // User clicks / reads the update notification: it should be removed from the list
    val updatedNotifications = notifications.filterNot { it.id == "update_1001" }
    assertEquals(1, updatedNotifications.size)
    assertFalse(updatedNotifications.any { it.id == "update_1001" })
  }

  @Test
  fun testAllClassStarRankingOrderDescending() {
    val students = listOf(
      com.example.data.model.Student("s1", "10", "Ayşe Ceylan", "Kız"),
      com.example.data.model.Student("s2", "25", "Mehmet Kaya", "Erkek"),
      com.example.data.model.Student("s3", "5", "Zeynep Demir", "Kız")
    )
    val starMap = mapOf("s1" to 15, "s2" to 20, "s3" to 15)

    val sorted = students.sortedWith { a, b ->
      val starA = starMap[a.id] ?: 0
      val starB = starMap[b.id] ?: 0
      if (starA != starB) {
        starB.compareTo(starA)
      } else {
        (a.studentNumber.toIntOrNull() ?: 0).compareTo(b.studentNumber.toIntOrNull() ?: 0)
      }
    }

    // Mehmet has 20 stars (Rank 1)
    assertEquals("s2", sorted[0].id)
    assertEquals("Mehmet Kaya", sorted[0].fullName)

    // s3 (no 5) and s1 (no 10) both have 15 stars -> s3 comes first because of student number 5 < 10
    assertEquals("s3", sorted[1].id)
    assertEquals("s1", sorted[2].id)
  }

  @Test
  fun testTimerModesAndFormatting() {
    val modes = com.example.ui.screens.TimerClassroomMode.values()
    assertEquals(4, modes.size)

    // Verify Quick mode defaults
    val quick = com.example.ui.screens.TimerClassroomMode.QUICK
    assertEquals(5, quick.defaultMinutes)
    assertEquals("Hızlı Sayaç", quick.title)
    assertTrue(quick.presetMinutes.contains(5))

    // Verify Exam mode defaults
    val exam = com.example.ui.screens.TimerClassroomMode.EXAM
    assertEquals(30, exam.defaultMinutes)
    assertEquals("Sınav Sayacı", exam.title)
    assertTrue(exam.presetMinutes.contains(40))

    // Formatting check
    val time300Sec = 300
    val m = time300Sec / 60
    val s = time300Sec % 60
    val formatted = String.format(java.util.Locale.US, "%02d:%02d", m, s)
    assertEquals("05:00", formatted)

    // Over an hour formatting check
    val time3665Sec = 3665
    val hours = time3665Sec / 3600
    val mins = (time3665Sec % 3600) / 60
    val secs = time3665Sec % 60
    val formattedLong = String.format(java.util.Locale.US, "%02d:%02d:%02d", hours, mins, secs)
    assertEquals("01:01:05", formattedLong)
  }

  @Test
  fun testSmartRegistrationNumberAssignment() {
    val helper = com.example.util.LibraryPersistenceHelper

    // 1. Empty list -> 1
    assertEquals(1, helper.findNextAvailableRegistrationNo(emptyList()))

    // 2. Sequential list [1, 2, 3] -> 4
    val seqBooks = listOf(
      com.example.data.model.Book(id = "b1", registrationNo = 1, name = "Kitap 1"),
      com.example.data.model.Book(id = "b2", registrationNo = 2, name = "Kitap 2"),
      com.example.data.model.Book(id = "b3", registrationNo = 3, name = "Kitap 3")
    )
    assertEquals(4, helper.findNextAvailableRegistrationNo(seqBooks))

    // 3. Gap inside [1, 2, 4] -> 3 (fills first hole)
    val gapBooks = listOf(
      com.example.data.model.Book(id = "b1", registrationNo = 1, name = "Kitap 1"),
      com.example.data.model.Book(id = "b2", registrationNo = 2, name = "Kitap 2"),
      com.example.data.model.Book(id = "b4", registrationNo = 4, name = "Kitap 4")
    )
    assertEquals(3, helper.findNextAvailableRegistrationNo(gapBooks))

    // 4. Missing #1 [2, 3, 5] -> 1
    val missingOne = listOf(
      com.example.data.model.Book(id = "b2", registrationNo = 2, name = "Kitap 2"),
      com.example.data.model.Book(id = "b3", registrationNo = 3, name = "Kitap 3"),
      com.example.data.model.Book(id = "b5", registrationNo = 5, name = "Kitap 5")
    )
    assertEquals(1, helper.findNextAvailableRegistrationNo(missingOne))
  }

  @Test
  fun testDuplicateBookNameDetection() {
    val helper = com.example.util.LibraryPersistenceHelper
    val existing = listOf(
      com.example.data.model.Book(id = "b1", registrationNo = 1, name = "Küçük Prens"),
      com.example.data.model.Book(id = "b2", registrationNo = 2, name = "Şeker Portakalı")
    )

    // Exact match
    assertTrue(helper.isBookNameDuplicate(existing, "Küçük Prens"))
    // Case-insensitive match
    assertTrue(helper.isBookNameDuplicate(existing, "küçük prens"))
    // Extra spaces
    assertTrue(helper.isBookNameDuplicate(existing, "  Küçük Prens  "))
    // Different book
    assertFalse(helper.isBookNameDuplicate(existing, "Sol Ayağım"))
    // Same book edit (excludeCurrentBookId)
    assertFalse(helper.isBookNameDuplicate(existing, "Küçük Prens", excludeCurrentBookId = "b1"))
  }

  @Test
  fun testReadingStarCalculation() {
    val helper = com.example.util.LibraryPersistenceHelper

    // 0 pages -> minimum 1 star
    assertEquals(1, helper.calculateReadingStars(0))
    // Under 10 pages -> 1 star
    assertEquals(1, helper.calculateReadingStars(9))
    // 10 pages -> 1 star
    assertEquals(1, helper.calculateReadingStars(10))
    // 11 pages -> 2 stars (tavan yuvarlama)
    assertEquals(2, helper.calculateReadingStars(11))
    // 21 pages -> 3 stars
    assertEquals(3, helper.calculateReadingStars(21))
    // 35 pages -> 4 stars
    assertEquals(4, helper.calculateReadingStars(35))
    // 45 pages -> 5 stars
    assertEquals(5, helper.calculateReadingStars(45))
    // 120 pages -> 12 stars
    assertEquals(12, helper.calculateReadingStars(120))
  }

  @Test
  fun testReadingAnalyticsKPIs() {
    val records = listOf(
      com.example.data.model.ReadingRecord(
        id = "r1",
        bookId = "b1",
        bookName = "Kitap A",
        pageCount = 60,
        studentId = "s1",
        studentName = "Ahmet",
        startDate = 1730419200000L,
        endDate = 1730592000000L
      ),
      com.example.data.model.ReadingRecord(
        id = "r2",
        bookId = "b2",
        bookName = "Kitap B",
        pageCount = 140,
        studentId = "s2",
        studentName = "Ayşe",
        startDate = 1730419200000L,
        endDate = 1730764800000L
      )
    )

    val totalBooks = records.size
    val totalPages = records.sumOf { it.pageCount }
    val starsSum = records.sumOf { com.example.util.LibraryPersistenceHelper.calculateReadingStars(it.pageCount) }

    assertEquals(2, totalBooks)
    assertEquals(200, totalPages)
    assertEquals(6 + 14, starsSum)
  }

  @Test
  fun testTournamentSmallestPowerOfTwo() {
    val engine = com.example.util.TournamentEngine
    assertEquals(2, engine.findSmallestPowerOfTwo(2))
    assertEquals(4, engine.findSmallestPowerOfTwo(3))
    assertEquals(4, engine.findSmallestPowerOfTwo(4))
    assertEquals(8, engine.findSmallestPowerOfTwo(5))
    assertEquals(8, engine.findSmallestPowerOfTwo(8))
    assertEquals(16, engine.findSmallestPowerOfTwo(9))
    assertEquals(16, engine.findSmallestPowerOfTwo(16))
    assertEquals(32, engine.findSmallestPowerOfTwo(17))
  }

  @Test
  fun testTournamentKnockoutFixtureWithBays() {
    val engine = com.example.util.TournamentEngine
    val participants = (1..5).map {
      Pair("s$it", "Öğrenci $it")
    }

    val matches = engine.generateKnockoutFixture(
      tournamentId = "t1",
      participants = participants,
      isDoubleMatch = false
    )

    // With 5 players, bracket size = 8.
    // 3 BAY players go directly to R2.
    // 1 match in R1 between the other 2 players.
    // Total matches in bracket = 1 (R1) + 2 (Semis) + 1 (Final) = 4 matches!
    assertEquals(4, matches.size)
    val r1Matches = matches.filter { it.round == 1 }
    assertEquals(1, r1Matches.size)
  }

  @Test
  fun testTournamentRoundRobinBergerRotation() {
    val engine = com.example.util.TournamentEngine
    val participants = (1..4).map {
      Pair("s$it", "Öğrenci $it")
    }

    // Tek maçlı lig
    val singleMatches = engine.generateRoundRobinFixture(
      tournamentId = "t2",
      participants = participants,
      isDoubleMatch = false
    )

    // 4 players: 3 rounds, 2 matches per round = 6 matches
    assertEquals(6, singleMatches.size)
    assertEquals(3, singleMatches.map { it.round }.distinct().size)

    // Çift maçlı lig (rövanşlı)
    val doubleMatches = engine.generateRoundRobinFixture(
      tournamentId = "t2_double",
      participants = participants,
      isDoubleMatch = true
    )
    assertEquals(12, doubleMatches.size)
    assertEquals(6, doubleMatches.map { it.round }.distinct().size)
  }

  @Test
  fun testTournamentStandingsOrdering() {
    val engine = com.example.util.TournamentEngine
    val participants = listOf(
      Pair("s1", "Ali"),
      Pair("s2", "Barış"),
      Pair("s3", "Can")
    )

    // Barış: 1 Win, +3 averaj
    // Ali: 1 Win, +1 averaj
    // Can: 2 Defeats
    val matches = listOf(
      com.example.data.model.TournamentMatch(
        id = "m1",
        tournamentId = "t_test",
        round = 1,
        matchCode = "M1",
        player1Id = "s2",
        player1Name = "Barış",
        player2Id = "s3",
        player2Name = "Can",
        score1 = 4,
        score2 = 1,
        winnerId = "s2",
        status = "completed"
      ),
      com.example.data.model.TournamentMatch(
        id = "m2",
        tournamentId = "t_test",
        round = 2,
        matchCode = "M2",
        player1Id = "s1",
        player1Name = "Ali",
        player2Id = "s3",
        player2Name = "Can",
        score1 = 2,
        score2 = 1,
        winnerId = "s1",
        status = "completed"
      )
    )

    val standings = engine.calculateStandings(participants, matches)
    assertEquals(3, standings.size)

    // Barış must be 1st (+3 goal diff vs +1 for Ali)
    assertEquals("s2", standings[0].playerId)
    assertEquals(3, standings[0].points)
    assertEquals(3, standings[0].goalDiff)

    // Ali must be 2nd
    assertEquals("s1", standings[1].playerId)
    assertEquals(3, standings[1].points)
    assertEquals(1, standings[1].goalDiff)

    // Can must be 3rd
    assertEquals("s3", standings[2].playerId)
    assertEquals(0, standings[2].points)
  }

  @Test
  fun testDoubleMatchTieBreakerDetection() {
    val engine = com.example.util.TournamentEngine
    val tournament = com.example.data.model.Tournament(
      id = "t_tb",
      classRoomId = "c1",
      name = "Futbol Turnuvası",
      matchType = com.example.data.model.TournamentMatchType.CIFT_MAC,
      winnerSelectionMethod = com.example.data.model.WinnerSelectionMethod.SCORE
    )

    val leg1 = com.example.data.model.TournamentMatch(
      id = "m1",
      tournamentId = "t_tb",
      round = 1,
      matchCode = "P1-1",
      player1Id = "s1",
      player1Name = "Ali",
      player2Id = "s2",
      player2Name = "Veli",
      score1 = 2,
      score2 = 0,
      winnerId = "s1",
      isDoubleMatch = true,
      pairCode = "PAIR-P1",
      status = "completed"
    )

    val leg2Tied = com.example.data.model.TournamentMatch(
      id = "m2",
      tournamentId = "t_tb",
      round = 2,
      matchCode = "P1-2",
      player1Id = "s2",
      player1Name = "Veli",
      player2Id = "s1",
      player2Name = "Ali",
      score1 = 2,
      score2 = 0,
      winnerId = "s2",
      isDoubleMatch = true,
      pairCode = "PAIR-P1",
      status = "completed"
    )

    // Both won 1 game, aggregate 2-2 -> Needs tie-breaker!
    val tieBreaker = engine.checkDoubleMatchTieBreaker(tournament, listOf(leg1, leg2Tied), leg2Tied)
    assertNotNull(tieBreaker)
    assertTrue(tieBreaker!!.isTieBreaker)

    val leg2NotTied = com.example.data.model.TournamentMatch(
      id = "m3",
      tournamentId = "t_tb",
      round = 2,
      matchCode = "P1-2",
      player1Id = "s2",
      player1Name = "Veli",
      player2Id = "s1",
      player2Name = "Ali",
      score1 = 1,
      score2 = 0,
      winnerId = "s2",
      isDoubleMatch = true,
      pairCode = "PAIR-P1",
      status = "completed"
    )

    // Aggregate: Ali 2 - 1 Veli -> Ali wins, no tie-breaker needed!
    val noTieBreaker = engine.checkDoubleMatchTieBreaker(tournament, listOf(leg1, leg2NotTied), leg2NotTied)
    assertNull(noTieBreaker)
  }
}




