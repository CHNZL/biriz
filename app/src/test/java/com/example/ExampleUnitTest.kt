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
  fun testCascadeBookDeletionLogic() {
    val bookA = com.example.data.model.Book(id = "b1", registrationNo = 1, name = "Küçük Prens", pageCount = 96)
    val bookB = com.example.data.model.Book(id = "b2", registrationNo = 2, name = "Momo", pageCount = 304)

    val readingRecords = listOf(
      com.example.data.model.ReadingRecord(id = "r1", bookId = "b1", bookName = "Küçük Prens", pageCount = 96, studentId = "s1", studentName = "Ahmet", startDate = 0L, endDate = 1000L, earnedStars = 10),
      com.example.data.model.ReadingRecord(id = "r2", bookId = "b2", bookName = "Momo", pageCount = 304, studentId = "s2", studentName = "Ayşe", startDate = 0L, endDate = 2000L, earnedStars = 31),
      com.example.data.model.ReadingRecord(id = "r3", bookId = "b1", bookName = "Küçük Prens", pageCount = 96, studentId = "s3", studentName = "Ali", startDate = 0L, endDate = 3000L, earnedStars = 10)
    )

    val remainingRecords = readingRecords.filter { it.bookId != bookA.id }
    val deletedRecords = readingRecords.filter { it.bookId == bookA.id }
    val revokedStars = deletedRecords.sumOf { it.earnedStars }

    assertEquals(1, remainingRecords.size)
    assertEquals("Momo", remainingRecords[0].bookName)
    assertEquals(2, deletedRecords.size)
    assertEquals(20, revokedStars)
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

    // With 5 players:
    // 1 match in R1 (Ön eleme) between 2 players.
    // 2 matches in R2 (Yarı Final).
    // 2 matches in R3 (1 Bronz Madalya / 3.lük maçı + 1 Şampiyonluk Finali) = 5 matches!
    assertEquals(5, matches.size)
    val r1Matches = matches.filter { it.round == 1 }
    assertEquals(1, r1Matches.size)
    assertTrue(matches.any { it.isThirdPlaceMatch })
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

  @Test
  fun testKnockoutAdvancementWithDecidingMatchAndThirdPlace() {
    val engine = com.example.util.TournamentEngine
    val tournament = com.example.data.model.Tournament(
      id = "t_ko",
      classRoomId = "c1",
      name = "Satranç",
      type = com.example.data.model.TournamentType.ELEME,
      matchType = com.example.data.model.TournamentMatchType.TEK_MAC,
      winnerSelectionMethod = com.example.data.model.WinnerSelectionMethod.WINNER
    )

    val participants = listOf(
      Pair("s1", "Ahmet"),
      Pair("s2", "Mehmet"),
      Pair("s3", "Ayşe"),
      Pair("s4", "Fatma")
    )

    val matches = engine.generateKnockoutFixture(
      tournamentId = "t_ko",
      participants = participants,
      isDoubleMatch = false
    )

    // 4 participants -> 2 Semis (R1), 1 3rd place match (R2), 1 Final (R2) = 4 matches
    assertEquals(4, matches.size)
    val thirdPlaceMatch = matches.find { it.isThirdPlaceMatch }
    assertNotNull(thirdPlaceMatch)

    // Complete semi-finals
    val s1Match = matches.find { it.matchCode == "R1-M1" }!!
    val s2Match = matches.find { it.matchCode == "R1-M2" }!!

    val completedS1 = s1Match.copy(
      status = "completed",
      winnerId = s1Match.player2Id // player 2 wins, player 1 loses
    )
    val completedS2 = s2Match.copy(
      status = "completed",
      winnerId = s2Match.player1Id // player 1 wins, player 2 loses
    )

    val matchesWithSemis = matches.map {
      when (it.id) {
        completedS1.id -> completedS1
        completedS2.id -> completedS2
        else -> it
      }
    }

    val synced = engine.syncKnockoutTree(matchesWithSemis, tournament.winnerSelectionMethod)

    // Check Final: Winners of semi-finals advanced!
    val finalMatch = synced.find { it.round == 2 && !it.isThirdPlaceMatch }!!
    assertEquals(completedS1.player2Id, finalMatch.player1Id)
    assertEquals(completedS2.player1Id, finalMatch.player2Id)

    // Check 3rd place match: Losers of semi-finals placed!
    val syncedThird = synced.find { it.isThirdPlaceMatch }!!
    assertEquals(completedS1.player1Id, syncedThird.player1Id)
    assertEquals(completedS2.player2Id, syncedThird.player2Id)
  }

  @Test
  fun testPodiumChampionshipAndThirdPlaceTieBreakers() {
    val engine = com.example.util.TournamentEngine
    val tournament = com.example.data.model.Tournament(
      id = "t_mangala",
      classRoomId = "c1",
      name = "Mangala Turnuvası",
      type = com.example.data.model.TournamentType.ELEME,
      matchType = com.example.data.model.TournamentMatchType.CIFT_MAC,
      winnerSelectionMethod = com.example.data.model.WinnerSelectionMethod.WINNER,
      participants = listOf("p_elif", "p_defne", "p_aras", "p_gunes")
    )

    // User scenario:
    // Final matches (Round 5):
    // R5-M1 (Elif vs Defne) -> Elif wins
    // R5-M1-R (Defne vs Elif) -> Defne wins
    // PAIR-R5-M1-T (Tie-Breaker: Elif vs Defne) -> Elif wins!
    val finalM1 = com.example.data.model.TournamentMatch(
      id = "m_f1",
      tournamentId = tournament.id,
      round = 5,
      player1Id = "p_elif",
      player2Id = "p_defne",
      player1Name = "ELİF YAZICI",
      player2Name = "DEFNE YARAMIŞ",
      matchCode = "R5-M1",
      isDoubleMatch = true,
      pairCode = "PAIR-R5-M1",
      isThirdPlaceMatch = false,
      status = "completed",
      winnerId = "p_elif",
      stage = "knockout"
    )
    val finalM2 = com.example.data.model.TournamentMatch(
      id = "m_f2",
      tournamentId = tournament.id,
      round = 5,
      player1Id = "p_defne",
      player2Id = "p_elif",
      player1Name = "DEFNE YARAMIŞ",
      player2Name = "ELİF YAZICI",
      matchCode = "R5-M1-R",
      isDoubleMatch = true,
      pairCode = "PAIR-R5-M1",
      isThirdPlaceMatch = false,
      status = "completed",
      winnerId = "p_defne",
      stage = "knockout"
    )
    val finalTieBreaker = com.example.data.model.TournamentMatch(
      id = "m_f_t",
      tournamentId = tournament.id,
      round = 5,
      player1Id = "p_elif",
      player2Id = "p_defne",
      player1Name = "ELİF YAZICI",
      player2Name = "DEFNE YARAMIŞ",
      matchCode = "PAIR-R5-M1-T",
      isDoubleMatch = true,
      pairCode = "PAIR-R5-M1",
      isTieBreaker = true,
      isThirdPlaceMatch = false,
      status = "completed",
      winnerId = "p_elif",
      stage = "knockout"
    )

    // 3.lük matches (Round 5):
    // R5-M-3RD (Güneş vs Aras) -> Güneş wins
    // R5-M-3RD-R (Aras vs Güneş) -> Aras wins
    // PAIR-R5-M-3RD-T (Tie-Breaker: Aras vs Güneş) -> Aras wins!
    val thirdM1 = com.example.data.model.TournamentMatch(
      id = "m_3_1",
      tournamentId = tournament.id,
      round = 5,
      player1Id = "p_gunes",
      player2Id = "p_aras",
      player1Name = "GÜNEŞ DEMİR",
      player2Name = "ARAS GÖKKAYA",
      matchCode = "R5-M-3RD",
      isDoubleMatch = true,
      pairCode = "PAIR-R5-M-3RD",
      isThirdPlaceMatch = true,
      status = "completed",
      winnerId = "p_gunes",
      stage = "knockout"
    )
    val thirdM2 = com.example.data.model.TournamentMatch(
      id = "m_3_2",
      tournamentId = tournament.id,
      round = 5,
      player1Id = "p_aras",
      player2Id = "p_gunes",
      player1Name = "ARAS GÖKKAYA",
      player2Name = "GÜNEŞ DEMİR",
      matchCode = "R5-M-3RD-R",
      isDoubleMatch = true,
      pairCode = "PAIR-R5-M-3RD",
      isThirdPlaceMatch = true,
      status = "completed",
      winnerId = "p_aras",
      stage = "knockout"
    )
    val thirdTieBreaker = com.example.data.model.TournamentMatch(
      id = "m_3_t",
      tournamentId = tournament.id,
      round = 5,
      player1Id = "p_aras",
      player2Id = "p_gunes",
      player1Name = "ARAS GÖKKAYA",
      player2Name = "GÜNEŞ DEMİR",
      matchCode = "PAIR-R5-M-3RD-T",
      isDoubleMatch = true,
      pairCode = "PAIR-R5-M-3RD",
      isTieBreaker = true,
      isThirdPlaceMatch = true,
      status = "completed",
      winnerId = "p_aras",
      stage = "knockout"
    )

    val allMatches = listOf(thirdTieBreaker, thirdM2, thirdM1, finalTieBreaker, finalM2, finalM1)

    val podium = engine.determinePodium(tournament, allMatches, emptyList())

    // 1. Şampiyon: Final maçı galibi ELİF YAZICI olmalıdır!
    assertNotNull(podium.first)
    assertEquals("p_elif", podium.first!!.first)
    assertEquals("ELİF YAZICI", podium.first!!.second)

    // 2. İkinci: Final maçı mağlubu DEFNE YARAMIŞ olmalıdır!
    assertNotNull(podium.second)
    assertEquals("p_defne", podium.second!!.first)
    assertEquals("DEFNE YARAMIŞ", podium.second!!.second)

    // 3. Üçüncü: 3.lük maçı galibi ARAS GÖKKAYA olmalıdır!
    assertNotNull(podium.third)
    assertEquals("p_aras", podium.third!!.first)
    assertEquals("ARAS GÖKKAYA", podium.third!!.second)

    // GÜNEŞ DEMİR (3.lük maçı kaybedeni) kürsüde OLMAMALIDIR!
    val podiumIds = listOf(podium.first?.first, podium.second?.first, podium.third?.first)
    assertFalse(podiumIds.contains("p_gunes"))
  }

  @Test
  fun testPreviousRoundsLockedWhenSubsequentRoundPlayed() {
    val engine = com.example.util.TournamentEngine
    val mR1_1 = com.example.data.model.TournamentMatch(
      id = "r1_m1",
      tournamentId = "t1",
      round = 1,
      player1Id = "p1",
      player2Id = "p2",
      matchCode = "R1-M1",
      status = "completed",
      stage = "knockout"
    )
    val mR1_2 = com.example.data.model.TournamentMatch(
      id = "r1_m2",
      tournamentId = "t1",
      round = 1,
      player1Id = "p3",
      player2Id = "p4",
      matchCode = "R1-M2",
      status = "completed",
      stage = "knockout"
    )
    val mR2_1 = com.example.data.model.TournamentMatch(
      id = "r2_m1",
      tournamentId = "t1",
      round = 2,
      player1Id = "p1",
      player2Id = "p3",
      matchCode = "R2-M1",
      status = "pending",
      stage = "knockout"
    )

    // Henüz 2. turda hiçbir maç tamamlanmadı -> 1. tur maçları KİLİTLİ DEĞİLDİR
    assertFalse(engine.isMatchLocked(mR1_1, listOf(mR1_1, mR1_2, mR2_1)))
    assertFalse(engine.isMatchLocked(mR2_1, listOf(mR1_1, mR1_2, mR2_1)))

    // 2. turda bir maç tamamlandığında -> 1. turun maçları KİLİTLENİR!
    val mR2_1_completed = mR2_1.copy(status = "completed", winnerId = "p1")
    val allWithR2Played = listOf(mR1_1, mR1_2, mR2_1_completed)

    assertTrue(engine.isMatchLocked(mR1_1, allWithR2Played))
    assertTrue(engine.isMatchLocked(mR1_2, allWithR2Played))
    // 2. turun kendisi kilitli değildir çünkü 3. tur henüz oynanmadı
    assertFalse(engine.isMatchLocked(mR2_1_completed, allWithR2Played))
  }

  @Test
  fun testParentPrivacyHelperKVKKAboluteAnonymization() {
    val privacyHelper = com.example.util.ParentPrivacyHelper
    val myChildId = "child_elif_14"
    val otherStudentId = "student_can_25"
    val linkedIds = setOf(myChildId)

    // 1. İsim maskeleme testi: Kendi çocuğu açık, diğer öğrenci "Öğrenci"
    assertEquals("Elif Yazıcı", privacyHelper.maskStudentName(myChildId, "Elif Yazıcı", linkedIds))
    assertEquals("Öğrenci", privacyHelper.maskStudentName(otherStudentId, "Can Demir", linkedIds))
    assertEquals("Öğrenci", privacyHelper.maskStudentName("", "", linkedIds))

    // 2. Numara maskeleme testi: Kendi çocuğu "14", diğer öğrenci "-"
    assertEquals("14", privacyHelper.maskStudentNumber(myChildId, "14", linkedIds))
    assertEquals("-", privacyHelper.maskStudentNumber(otherStudentId, "25", linkedIds))

    // 3. Sıralama (Standings) maskeleme testi
    val standings = listOf(
      com.example.data.model.TournamentStanding(otherStudentId, "Can Demir", 5, 5, 0, 0, 10, 0, 10, 15),
      com.example.data.model.TournamentStanding(myChildId, "Elif Yazıcı", 5, 4, 0, 1, 8, 2, 6, 12)
    )
    val maskedStandings = privacyHelper.maskStandings(standings, linkedIds)
    assertEquals("Öğrenci", maskedStandings[0].playerName)
    assertEquals("Elif Yazıcı", maskedStandings[1].playerName)

    // 4. Turnuva Maçı Eşleşme maskeleme testi
    val match = com.example.data.model.TournamentMatch(
      id = "m1",
      tournamentId = "t1",
      round = 1,
      player1Id = myChildId,
      player2Id = otherStudentId,
      player1Name = "Elif Yazıcı",
      player2Name = "Can Demir",
      matchCode = "R1-M1"
    )
    val maskedMatch = privacyHelper.maskTournamentMatch(match, linkedIds)
    assertEquals("Elif Yazıcı", maskedMatch.player1Name)
    assertEquals("Öğrenci", maskedMatch.player2Name)

    // 5. Oturma planı sıra gösterim testi
    val myStudent = com.example.data.model.Student(myChildId, "c1", "Elif Yazıcı", "14", "Kız")
    val otherStudent = com.example.data.model.Student(otherStudentId, "c1", "Can Demir", "25", "Erkek")

    val childDeskInfo = privacyHelper.getDeskDisplayInfo(myChildId, myStudent, linkedIds)
    assertTrue(childDeskInfo.second) // isChild = true
    assertEquals("Elif Yazıcı", childDeskInfo.first)

    val otherDeskInfo = privacyHelper.getDeskDisplayInfo(otherStudentId, otherStudent, linkedIds)
    assertFalse(otherDeskInfo.second) // isChild = false
    assertEquals("Öğrenci", otherDeskInfo.first)
  }

  @Test
  fun testParentLinkEvaluationAndSync() {
    val unlinkedStudent = com.example.data.model.Student(
      id = "std_test_1",
      studentNumber = "45",
      fullName = "SEMİH AHMET KAVAKOĞLU",
      gender = "Erkek",
      parentInviteCode = "TD8GDLU8",
      parentName = null,
      parentEmail = null,
      linkedParents = emptyList()
    )
    assertFalse(unlinkedStudent.isParentLinked)
    assertEquals(0, unlinkedStudent.allLinkedParents.size)

    val linkedStudent = unlinkedStudent.copy(
      parentName = "Ahmet Kavakoğlu",
      parentEmail = "ahmet.kav@gmail.com",
      parentLinkedAt = "09 Kasım 2026, 14:00",
      parentLastLoginAt = "Bugün, 09:15"
    )
    assertTrue(linkedStudent.isParentLinked)
    assertEquals(1, linkedStudent.allLinkedParents.size)
    assertEquals("ahmet.kav@gmail.com", linkedStudent.allLinkedParents.first().email)
    assertEquals("Ahmet Kavakoğlu", linkedStudent.allLinkedParents.first().name)

    // Test ClassRegistryRepository sync
    val repo = com.example.data.repository.ClassRegistryRepository()
    repo.seedDefaultTestClassroom("test_uid")

    // Find student 45 by code
    val result = repo.findInSampleStudents(
      sanitizedCode = "94K7P2MN",
      currentUserEmail = "veli.test@gmail.com",
      currentUserFullName = "Veli Test"
    )
    assertNotNull(result)
    val (matchedStudent, _) = result!!
    assertTrue(matchedStudent.isParentLinked)
    assertEquals("veli.test@gmail.com", matchedStudent.parentEmail)

    // Verify repository's state flow is updated
    val updatedInRepo = repo.students.value.firstOrNull { it.id == matchedStudent.id }
    assertNotNull(updatedInRepo)
    assertTrue(updatedInRepo!!.isParentLinked)
    assertEquals("veli.test@gmail.com", updatedInRepo.parentEmail)

    // Test syncLinkedStudentsWithParent
    repo.syncLinkedStudentsWithParent(
      linkedStudentIds = listOf("std_326"),
      parentEmail = "gunes.veli@test.com",
      parentName = "Güneş Velisi"
    )
    val gunesInRepo = repo.students.value.firstOrNull { it.id == "std_326" }
    assertNotNull(gunesInRepo)
    assertTrue(gunesInRepo!!.isParentLinked)
    assertEquals("gunes.veli@test.com", gunesInRepo.parentEmail)
    assertEquals("Güneş Velisi", gunesInRepo.parentName)
  }

  @Test
  fun testAutoRoutingParentDetection() = kotlinx.coroutines.runBlocking {
    val repo = com.example.data.repository.ClassRegistryRepository()
    repo.seedDefaultTestClassroom("test_teacher_uid")

    // Link a student with a parent email
    repo.syncLinkedStudentsWithParent(
      linkedStudentIds = listOf("std_45"),
      parentEmail = "cihan.veli@gmail.com",
      parentName = "Cihan Veli"
    )

    // Query parent linked students by email
    val linkedStudents = repo.findStudentsLinkedToParent("cihan.veli@gmail.com")
    assertEquals(1, linkedStudents.size)
    val (foundStudent, foundClass) = linkedStudents.first()
    assertEquals("std_45", foundStudent.id)
    assertEquals("45", foundStudent.studentNumber)
    assertTrue(foundStudent.allLinkedParents.any { it.email == "cihan.veli@gmail.com" })
    assertNotNull(foundClass)

    // Unlinked email should return empty
    val unlinked = repo.findStudentsLinkedToParent("nonexistent.parent@gmail.com")
    assertTrue(unlinked.isEmpty())
  }

  @Test
  fun testParentPresenceAndInstantDisconnect() = kotlinx.coroutines.runBlocking {
    val now = System.currentTimeMillis()
    val repo = com.example.data.repository.ClassRegistryRepository()
    val sampleClass = repo.seedDefaultTestClassroom("test_teacher_uid")

    // Link student 402 with a parent
    val linkResult = repo.findInSampleStudents(
      sanitizedCode = "TD8GDLU8",
      currentUserEmail = "cihanogretmeniletisim@gmail.com",
      currentUserFullName = "Cihan Öğretmen İLETİŞİM"
    )
    assertNotNull(linkResult)
    val linkedStudent = linkResult!!.first
    assertTrue(linkedStudent.isParentLinked)
    assertTrue(linkedStudent.isParentCurrentlyActive(System.currentTimeMillis()))

    // When parent goes offline, isParentCurrentlyActive must be false and resolvedParentLastLoginDisplay must show the real date/time
    repo.updateParentPresence(
      classroomId = sampleClass.id,
      studentIds = listOf(linkedStudent.id),
      parentEmail = "cihanogretmeniletisim@gmail.com",
      parentName = "Cihan Öğretmen İLETİŞİM",
      isOnline = false
    )
    val offlineStudent = repo.students.value.first { it.id == linkedStudent.id }
    assertFalse(offlineStudent.isParentCurrentlyActive(System.currentTimeMillis()))
    assertTrue(offlineStudent.resolvedParentLastLoginDisplay().isNotBlank())
    assertFalse(offlineStudent.resolvedParentLastLoginDisplay().startsWith("Bugün"))

    // When teacher disconnects parent ("Bağlantıyı Kaldır" -> Evet), isParentLinked must immediately become false and linkedParents must be empty
    repo.updateStudentParentInfo(
      classroomId = sampleClass.id,
      studentId = linkedStudent.id,
      parentName = null,
      parentEmail = null,
      parentLinkedAt = null,
      parentLastLoginAt = null
    )
    val disconnectedStudent = repo.students.value.first { it.id == linkedStudent.id }
    assertFalse(disconnectedStudent.isParentLinked)
    assertTrue(disconnectedStudent.allLinkedParents.isEmpty())
    assertTrue(disconnectedStudent.linkedParents.isEmpty())

    // Default Teacher dashboard sub-page must be ANA_SAYFA
    val initialUiState = com.example.ui.viewmodel.BirizUiState()
    assertEquals(com.example.ui.viewmodel.DashboardSubPage.ANA_SAYFA, initialUiState.currentSubPage)
  }

  @Test
  fun testSampleChatMessageFilteringDoesNotDropRealMessages() {
    val sampleMsg1 = com.example.data.model.ChatMessage(
      id = "msg_1",
      classId = "c1",
      studentId = "s1",
      studentName = "Ali",
      studentNumber = "1",
      parentName = "Ali Velisi",
      parentEmail = "p@test.com",
      isFromTeacher = true,
      senderName = "Öğretmen",
      text = "Örnek mesaj",
      timestamp = 1700000000000L
    )
    val realTimestampMsg = sampleMsg1.copy(id = "msg_1794500000000", text = "Gerçek mesaj")
    val uuidChatMsg = sampleMsg1.copy(id = "chat_3f9b2c1a-4d5e-4f6a-9b1c-1234567890ab", text = "UUID mesaj")

    assertTrue(com.example.util.CommunicationPersistenceHelper.isSampleChatMessage(sampleMsg1))
    assertFalse(com.example.util.CommunicationPersistenceHelper.isSampleChatMessage(realTimestampMsg))
    assertFalse(com.example.util.CommunicationPersistenceHelper.isSampleChatMessage(uuidChatMsg))
  }

  @Test
  fun testStaleUpdateNotificationAutoPruningByVersionAndCheckState() {
    val oldNotif = com.example.data.model.BirizNotification(
      id = "app_update_1084",
      type = com.example.data.model.NotificationType.APP_UPDATE,
      title = "Yeni Güncelleme Hazır (v1.0.84)",
      message = "Güncelleme açıklaması",
      timestamp = 100L,
      isRead = false,
      targetId = "1084",
      targetSubPageName = "APP_UPDATE"
    )
    val sameVersionNotif = oldNotif.copy(
      id = "app_update_1085",
      title = "Yeni Güncelleme Hazır (v1.0.85)",
      targetId = "1085"
    )
    val newerVersionNotif = oldNotif.copy(
      id = "app_update_1086",
      title = "Yeni Güncelleme Hazır (v1.0.86)",
      targetId = "1086"
    )
    val normalMessageNotif = com.example.data.model.BirizNotification(
      id = "msg_notif_std_1",
      type = com.example.data.model.NotificationType.MESSAGE,
      title = "Yeni Veli Mesajı",
      message = "Merhaba hocam",
      timestamp = 200L,
      isRead = false,
      targetId = "std_1",
      targetSubPageName = "ILETISIM_MESAJLAR"
    )

    val allNotifs = listOf(oldNotif, sameVersionNotif, newerVersionNotif, normalMessageNotif)

    // Installed v1.0.85 (code 1085) with available remote v1.0.86 -> keeps only 1086 + normal message
    val remoteNewerAvailable = com.example.util.UpdateInfo(
      isUpdateAvailable = true,
      currentVersionName = "1.0.85",
      currentVersionCode = 1085,
      latestVersionName = "1.0.86",
      latestVersionCode = 1086,
      downloadUrl = "https://example.com/app.apk",
      releaseNotes = "Yeni sürüm"
    )
    val prunedWhileNewerAvailable = com.example.util.NotificationLifecycleHelper.pruneStaleUpdateNotifications(
      notifications = allNotifs,
      currentVersionCode = 1085,
      currentVersionName = "1.0.85",
      latestCheckInfo = remoteNewerAvailable
    )
    assertEquals(2, prunedWhileNewerAvailable.size)
    assertTrue(prunedWhileNewerAvailable.any { it.id == "app_update_1086" })
    assertTrue(prunedWhileNewerAvailable.any { it.id == "msg_notif_std_1" })

    // When update check confirms app is already up-to-date -> removes ALL APP_UPDATE notifications
    val alreadyUpToDate = com.example.util.UpdateInfo(
      isUpdateAvailable = false,
      currentVersionName = "1.0.85",
      currentVersionCode = 1085,
      latestVersionName = "1.0.85",
      latestVersionCode = 1085,
      downloadUrl = "",
      releaseNotes = ""
    )
    val prunedWhenUpToDate = com.example.util.NotificationLifecycleHelper.pruneStaleUpdateNotifications(
      notifications = allNotifs,
      currentVersionCode = 1085,
      currentVersionName = "1.0.85",
      latestCheckInfo = alreadyUpToDate
    )
    assertEquals(1, prunedWhenUpToDate.size)
    assertEquals("msg_notif_std_1", prunedWhenUpToDate.first().id)
  }

  @Test
  fun testChatScreenActiveSuppressionAndNotificationRemoval() {
    // Teacher actively viewing std_1 chat in foreground -> suppress notification
    assertTrue(
      com.example.util.NotificationLifecycleHelper.isChatScreenActivelyOpenForStudent(
        studentId = "std_1",
        viewerIsTeacher = true,
        viewerIsParent = false,
        isAppInForeground = true,
        isTeacherChatPageOpen = true,
        activeTeacherChatStudentId = "std_1",
        isParentCommTabActive = false,
        isParentChatSubTabActive = false,
        activeParentStudentId = null,
        isNotificationCenterOpen = false
      )
    )

    // Teacher viewing std_2 while message arrives for std_1 -> do NOT suppress
    assertFalse(
      com.example.util.NotificationLifecycleHelper.isChatScreenActivelyOpenForStudent(
        studentId = "std_1",
        viewerIsTeacher = true,
        viewerIsParent = false,
        isAppInForeground = true,
        isTeacherChatPageOpen = true,
        activeTeacherChatStudentId = "std_2",
        isParentCommTabActive = false,
        isParentChatSubTabActive = false,
        activeParentStudentId = null,
        isNotificationCenterOpen = false
      )
    )

    // Parent actively viewing Chat sub-tab for std_1 in foreground -> suppress notification
    assertTrue(
      com.example.util.NotificationLifecycleHelper.isChatScreenActivelyOpenForStudent(
        studentId = "std_1",
        viewerIsTeacher = false,
        viewerIsParent = true,
        isAppInForeground = true,
        isTeacherChatPageOpen = false,
        activeTeacherChatStudentId = null,
        isParentCommTabActive = true,
        isParentChatSubTabActive = true,
        activeParentStudentId = "std_1",
        isNotificationCenterOpen = false
      )
    )

    // Parent on Announcements sub-tab -> do NOT suppress chat notification
    assertFalse(
      com.example.util.NotificationLifecycleHelper.isChatScreenActivelyOpenForStudent(
        studentId = "std_1",
        viewerIsTeacher = false,
        viewerIsParent = true,
        isAppInForeground = true,
        isTeacherChatPageOpen = false,
        activeTeacherChatStudentId = null,
        isParentCommTabActive = true,
        isParentChatSubTabActive = false,
        activeParentStudentId = "std_1",
        isNotificationCenterOpen = false
      )
    )

    // Notification center open or app in background -> do NOT suppress
    assertFalse(
      com.example.util.NotificationLifecycleHelper.isChatScreenActivelyOpenForStudent(
        studentId = "std_1",
        viewerIsTeacher = false,
        viewerIsParent = true,
        isAppInForeground = false,
        isTeacherChatPageOpen = false,
        activeTeacherChatStudentId = null,
        isParentCommTabActive = true,
        isParentChatSubTabActive = true,
        activeParentStudentId = "std_1",
        isNotificationCenterOpen = false
      )
    )

    val notifs = listOf(
      com.example.data.model.BirizNotification(
        id = "msg_notif_std_1",
        type = com.example.data.model.NotificationType.MESSAGE,
        title = "Mesaj",
        message = "Merhaba",
        timestamp = 1L,
        targetSubPageName = "ILETISIM_MESAJLAR",
        targetId = "std_1"
      ),
      com.example.data.model.BirizNotification(
        id = "msg_notif_std_2",
        type = com.example.data.model.NotificationType.MESSAGE,
        title = "Mesaj 2",
        message = "Selam",
        timestamp = 2L,
        targetSubPageName = "ILETISIM_MESAJLAR",
        targetId = "std_2"
      )
    )
    val remaining = com.example.util.NotificationLifecycleHelper.removeMessageNotificationsForStudent(notifs, "std_1")
    assertEquals(1, remaining.size)
    assertEquals("msg_notif_std_2", remaining.first().id)
  }

  @Test
  fun testIncomingMessageViewerMatching() {
    val teacherMsg = com.example.data.model.ChatMessage(
      id = "chat_1",
      classId = "c1",
      studentId = "std_1",
      studentName = "Ali",
      studentNumber = "1",
      parentName = "Ali Velisi",
      parentEmail = "veli@example.com",
      isFromTeacher = true,
      senderName = "Cihan Öğretmen",
      text = "Merhaba",
      timestamp = 100L
    )
    val parentMsg = teacherMsg.copy(id = "chat_2", isFromTeacher = false, senderName = "Ali Velisi")

    // Teacher sees parent messages as incoming, never teacher messages
    assertFalse(
      com.example.util.NotificationLifecycleHelper.isIncomingMessageForViewer(
        msg = teacherMsg,
        viewerIsTeacher = true,
        viewerIsParent = false,
        linkedParentStudentIds = emptyList(),
        currentUserEmail = "ogretmen@example.com"
      )
    )
    assertTrue(
      com.example.util.NotificationLifecycleHelper.isIncomingMessageForViewer(
        msg = parentMsg,
        viewerIsTeacher = true,
        viewerIsParent = false,
        linkedParentStudentIds = emptyList(),
        currentUserEmail = "ogretmen@example.com"
      )
    )

    // Linked parent sees matching student teacher messages, not own parent messages or other students
    assertTrue(
      com.example.util.NotificationLifecycleHelper.isIncomingMessageForViewer(
        msg = teacherMsg,
        viewerIsTeacher = false,
        viewerIsParent = true,
        linkedParentStudentIds = listOf("std_1"),
        currentUserEmail = "veli@example.com"
      )
    )
    assertFalse(
      com.example.util.NotificationLifecycleHelper.isIncomingMessageForViewer(
        msg = parentMsg,
        viewerIsTeacher = false,
        viewerIsParent = true,
        linkedParentStudentIds = listOf("std_1"),
        currentUserEmail = "veli@example.com"
      )
    )
    assertFalse(
      com.example.util.NotificationLifecycleHelper.isIncomingMessageForViewer(
        msg = teacherMsg.copy(studentId = "std_other"),
        viewerIsTeacher = false,
        viewerIsParent = true,
        linkedParentStudentIds = listOf("std_1"),
        currentUserEmail = "veli@example.com"
      )
    )
  }

  @Test
  fun testPerUserMessageDeletionIsolation() {
    val msg = com.example.data.model.ChatMessage(
      id = "msg_100",
      classId = "c1",
      studentId = "std_1",
      studentName = "Ali",
      studentNumber = "1",
      parentName = "Ali Velisi",
      parentEmail = "veli1@example.com",
      isFromTeacher = true,
      senderName = "Öğretmen",
      text = "Ödev kontrolü",
      timestamp = 1_000L
    )

    // Öğretmen "Benden Sil" yaptığında sadece öğretmenden gizlenir; velide görünmeye devam eder
    val hiddenFromTeacher = msg.copy(hiddenForTeacher = true)
    assertFalse(
      com.example.util.ChatVisibilityHelper.isMessageVisibleToViewer(
        msg = hiddenFromTeacher,
        viewerIsTeacher = true,
        viewerIsParent = false,
        currentParentEmail = "ogretmen@example.com",
        clearTimestamps = emptyMap()
      )
    )
    assertTrue(
      com.example.util.ChatVisibilityHelper.isMessageVisibleToViewer(
        msg = hiddenFromTeacher,
        viewerIsTeacher = false,
        viewerIsParent = true,
        currentParentEmail = "veli1@example.com",
        clearTimestamps = emptyMap()
      )
    )

    // Veli "Benden Sil" yaptığında sadece o veliden gizlenir; öğretmende ve 2. velide görünmeye devam eder
    val hiddenFromParent1 = msg.copy(hiddenForParentEmails = listOf("veli1@example.com"))
    assertFalse(
      com.example.util.ChatVisibilityHelper.isMessageVisibleToViewer(
        msg = hiddenFromParent1,
        viewerIsTeacher = false,
        viewerIsParent = true,
        currentParentEmail = "veli1@example.com",
        clearTimestamps = emptyMap()
      )
    )
    assertTrue(
      com.example.util.ChatVisibilityHelper.isMessageVisibleToViewer(
        msg = hiddenFromParent1,
        viewerIsTeacher = true,
        viewerIsParent = false,
        currentParentEmail = "ogretmen@example.com",
        clearTimestamps = emptyMap()
      )
    )
    assertTrue(
      com.example.util.ChatVisibilityHelper.isMessageVisibleToViewer(
        msg = hiddenFromParent1,
        viewerIsTeacher = false,
        viewerIsParent = true,
        currentParentEmail = "veli2@example.com",
        clearTimestamps = emptyMap()
      )
    )
  }

  @Test
  fun testClearChatHistoryWatermarkIsolation() {
    val oldMsg = com.example.data.model.ChatMessage(
      id = "msg_old",
      classId = "c1",
      studentId = "std_1",
      studentName = "Ali",
      studentNumber = "1",
      parentName = "Veli",
      parentEmail = "veli1@example.com",
      isFromTeacher = false,
      senderName = "Veli",
      text = "Eski mesaj",
      timestamp = 1_000L
    )
    val newMsg = oldMsg.copy(id = "msg_new", text = "Yeni mesaj", timestamp = 2_000L)

    // Öğretmen 1500ms'de std_1|veli1@example.com sohbetini temizlerse
    val clearKeys = com.example.util.ChatVisibilityHelper.teacherConversationClearKeys(
      studentId = "std_1",
      parentEmail = "veli1@example.com",
      isSingleParentStudent = false
    )
    assertEquals(listOf("std_1|veli1@example.com"), clearKeys)
    val teacherClearMap = mapOf("std_1|veli1@example.com" to 1_500L)

    val visibleForTeacher = com.example.util.ChatVisibilityHelper.filterVisibleMessages(
      messages = listOf(oldMsg, newMsg),
      viewerIsTeacher = true,
      viewerIsParent = false,
      currentParentEmail = "ogretmen@example.com",
      clearTimestamps = teacherClearMap
    )
    assertEquals(listOf("msg_new"), visibleForTeacher.map { it.id })

    // Veli geçmişi temizlemediği için eski + yeni tüm mesajları görmeye devam eder
    val visibleForParent = com.example.util.ChatVisibilityHelper.filterVisibleMessages(
      messages = listOf(oldMsg, newMsg),
      viewerIsTeacher = false,
      viewerIsParent = true,
      currentParentEmail = "veli1@example.com",
      clearTimestamps = emptyMap()
    )
    assertEquals(listOf("msg_old", "msg_new"), visibleForParent.map { it.id })
  }

  @Test
  fun testChatDateDividerLabels() {
    val cal = java.util.Calendar.getInstance()
    cal.set(2026, java.util.Calendar.MAY, 15, 14, 30, 0)
    val now = cal.timeInMillis
    val sameDayEarlier = now - 3_600_000L
    val yesterday = now - 86_400_000L

    assertEquals("Bugün", com.example.util.ChatVisibilityHelper.formatChatDateDivider(sameDayEarlier, now))
    assertEquals("Dün", com.example.util.ChatVisibilityHelper.formatChatDateDivider(yesterday, now))
  }

  @Test
  fun testActivityNotificationsAllNineEventsAndDeduplication() {
    val helper = com.example.util.ActivityNotificationHelper
    val baseline = 10_000L
    val now = 20_000L
    val students = listOf(
      com.example.data.model.Student(id = "std_1", studentNumber = "12", fullName = "Ali Yılmaz", gender = "Erkek"),
      com.example.data.model.Student(id = "std_2", studentNumber = "18", fullName = "Zeynep Demir", gender = "Kız"),
      com.example.data.model.Student(id = "std_3", studentNumber = "21", fullName = "Mehmet Kaya", gender = "Erkek"),
      com.example.data.model.Student(id = "std_4", studentNumber = "25", fullName = "Elif Can", gender = "Kız")
    )

    // Pre-baseline old items must be silently recorded without generating bell notifications
    val oldSnapshot = com.example.util.ActivityNotificationHelper.ActivitySnapshot(
      announcements = listOf(
        com.example.data.model.Announcement(
          id = "ann_old",
          classId = "c1",
          title = "Eski Duyuru",
          content = "Geçen haftadan",
          targetType = com.example.data.model.AnnouncementTargetType.ALL_CLASS,
          createdAt = 1_000L,
          updatedAt = 1_000L
        )
      ),
      books = listOf(
        com.example.data.model.Book(
          id = "book_old",
          registrationNo = 1,
          name = "Eski Kitap",
          createdAt = 2_000L,
          updatedAt = 2_000L
        )
      ),
      students = students
    )
    val initialRes = helper.evaluateParentNotifications(
      previous = null,
      current = oldSnapshot,
      linkedStudentIds = setOf("std_1"),
      seenEventKeys = emptySet(),
      baselineTimestampMs = baseline,
      nowMs = now
    )
    assertTrue("Old items before baseline must not notify", initialRes.newNotifications.isEmpty())
    assertTrue(initialRes.updatedSeenKeys.contains("ann_ann_old_1000"))

    // Now trigger active events after baseline (adding a book alone must NOT notify)
    val fullSnapshot = com.example.util.ActivityNotificationHelper.ActivitySnapshot(
      announcements = oldSnapshot.announcements + listOf(
        com.example.data.model.Announcement(
          id = "ann_new",
          classId = "c1",
          title = "Gezi İzni",
          content = "Yarın müze gezimiz var.",
          targetType = com.example.data.model.AnnouncementTargetType.ALL_CLASS,
          createdAt = 12_000L,
          updatedAt = 12_000L
        ),
        com.example.data.model.Announcement(
          id = "ann_other",
          classId = "c1",
          title = "Diğer Öğrenci Duyurusu",
          content = "Sadece std_2 için",
          targetType = com.example.data.model.AnnouncementTargetType.SPECIFIC_STUDENTS,
          targetStudentIds = listOf("std_2"),
          createdAt = 12_500L,
          updatedAt = 12_500L
        )
      ),
      starRecords = listOf(
        com.example.data.model.StarRewardRecord(
          id = "star_1",
          classId = "c1",
          studentId = "std_1",
          studentName = "Ali Yılmaz",
          studentNumber = "12",
          categoryId = "cat_math",
          categoryName = "Matematik",
          starCount = 3,
          note = "Harika çözüm",
          timestamp = 13_000L
        ),
        com.example.data.model.StarRewardRecord(
          id = "star_other",
          classId = "c1",
          studentId = "std_2",
          studentName = "Zeynep Demir",
          studentNumber = "18",
          categoryId = "cat_tr",
          categoryName = "Türkçe",
          starCount = 5,
          note = "",
          timestamp = 13_100L
        )
      ),
      books = listOf(
        oldSnapshot.books.first().copy(
          activeAssignments = listOf(
            com.example.data.model.ActiveAssignment("std_1", "Ali Yılmaz", 14_000L)
          ),
          status = "Okunuyor",
          updatedAt = 14_000L
        ),
        com.example.data.model.Book(
          id = "book_new",
          registrationNo = 2,
          name = "Küçük Prens",
          author = "Antoine de Saint-Exupéry",
          pageCount = 96,
          createdAt = 13_500L,
          updatedAt = 13_500L
        )
      ),
      readingRecords = listOf(
        com.example.data.model.ReadingRecord(
          id = "read_1",
          bookId = "book_new",
          bookName = "Küçük Prens",
          pageCount = 96,
          studentId = "std_1",
          studentName = "Ali Yılmaz",
          startDate = 13_600L,
          endDate = 14_500L,
          earnedStars = 10,
          createdAt = 14_500L
        )
      ),
      tournaments = listOf(
        com.example.data.model.Tournament(
          id = "tour_1",
          classRoomId = "c1",
          name = "Bahar Satranç Turnuvası",
          type = com.example.data.model.TournamentType.ELEME,
          participants = listOf("std_1", "std_2", "std_3", "std_4"),
          status = com.example.data.model.TournamentStatus.TAMAMLANDI,
          winnerId = "std_1",
          runnerUpId = "std_3",
          thirdPlaceId = "std_2",
          createdAt = 15_000L,
          updatedAt = 18_000L
        )
      ),
      matchesByTournament = mapOf(
        "tour_1" to listOf(
          com.example.data.model.TournamentMatch(
            id = "m_sf1",
            tournamentId = "tour_1",
            round = 1,
            stage = "knockout",
            matchCode = "R1-M1",
            player1Id = "std_1",
            player1Name = "Ali Yılmaz",
            player2Id = "std_2",
            player2Name = "Zeynep Demir",
            score1 = 2,
            score2 = 0,
            winnerId = "std_1",
            status = "completed",
            createdAt = 16_000L
          ),
          com.example.data.model.TournamentMatch(
            id = "m_final",
            tournamentId = "tour_1",
            round = 2,
            stage = "knockout",
            matchCode = "R2-M1",
            player1Id = "std_1",
            player1Name = "Ali Yılmaz",
            player2Id = "std_3",
            player2Name = "Mehmet Kaya",
            score1 = 2,
            score2 = 1,
            winnerId = "std_1",
            status = "completed",
            createdAt = 17_000L
          )
        )
      ),
      students = students
    )

    val evalRes = helper.evaluateParentNotifications(
      previous = oldSnapshot,
      current = fullSnapshot,
      linkedStudentIds = setOf("std_1"),
      seenEventKeys = initialRes.updatedSeenKeys,
      baselineTimestampMs = baseline,
      nowMs = now
    )

    val titles = evalRes.newNotifications.map { it.title }
    // Other students' targeted items must not leak
    assertFalse(evalRes.newNotifications.any { it.message.contains("Zeynep Demir +5") || it.id.contains("ann_other") || it.id.contains("star_other") })
    // 1. Duyuru yayınlandı
    assertTrue(titles.any { it.contains("Yeni Sınıf Duyurusu") })
    // 2. Yıldız eklendi
    assertTrue(titles.any { it.contains("Yıldız Kazandı") })
    // 3. Kitaplığa yeni kitap eklendiğinde bildirim OLUŞMAMALI
    assertFalse("Adding a new book must not generate a notification", titles.any { it.contains("Yeni Kitap") })
    // 4. Kitap ödünç alındı
    assertTrue(titles.any { it.contains("Ödünç Alındı") })
    // 5. Kitap iade edildi
    assertTrue(titles.any { it.contains("İade Edildi") })
    // 6. Turnuvaya katıldı
    assertTrue(titles.any { it.contains("Turnuvaya Katıldı") })
    // 7. Turnuvada maç yaptı (2 maç tamamladı -> 2 maç bildirimi)
    assertEquals(2, titles.count { it.contains("Turnuva Maçı Tamamlandı") })
    // 8. Turnuvada tur atladı (Round 2 Final)
    assertTrue(evalRes.newNotifications.any { it.title.contains("Tur Atladı") && it.message.contains("Final") })
    // 9. Turnuvayı ne olarak / kaçıncı bitirdi
    assertTrue(evalRes.newNotifications.any { it.title.contains("Turnuva Sonuçlandı") && it.message.contains("1. sırada") && it.message.contains("Şampiyon") })

    // Re-evaluating the same snapshot must NOT generate duplicate notifications
    val repeatRes = helper.evaluateParentNotifications(
      previous = fullSnapshot,
      current = fullSnapshot,
      linkedStudentIds = setOf("std_1"),
      seenEventKeys = evalRes.updatedSeenKeys,
      baselineTimestampMs = baseline,
      nowMs = now + 1_000L
    )
    assertTrue("No duplicate notifications on repeated snapshot", repeatRes.newNotifications.isEmpty())
  }

  @Test
  fun testTournamentPlacementDescriptionsAcrossFormats() {
    val helper = com.example.util.ActivityNotificationHelper
    val students = (1..8).map {
      com.example.data.model.Student(
        id = "s$it",
        studentNumber = "$it",
        fullName = "Öğrenci $it",
        gender = if (it % 2 == 0) "Kız" else "Erkek"
      )
    }

    // 1. Knockout podium & 4th place & Quarter Finalist
    val koTournament = com.example.data.model.Tournament(
      id = "t_ko_place",
      classRoomId = "c1",
      name = "Mangala Kupası",
      type = com.example.data.model.TournamentType.ELEME,
      participants = (1..8).map { "s$it" },
      status = com.example.data.model.TournamentStatus.TAMAMLANDI,
      winnerId = "s1",
      runnerUpId = "s2",
      thirdPlaceId = "s3"
    )
    val koMatches = listOf(
      com.example.data.model.TournamentMatch(id = "qf1", tournamentId = "t_ko_place", round = 1, stage = "knockout", matchCode = "R1-M1", player1Id = "s1", player2Id = "s5", winnerId = "s1", status = "completed"),
      com.example.data.model.TournamentMatch(id = "qf2", tournamentId = "t_ko_place", round = 1, stage = "knockout", matchCode = "R1-M2", player1Id = "s2", player2Id = "s6", winnerId = "s2", status = "completed"),
      com.example.data.model.TournamentMatch(id = "qf3", tournamentId = "t_ko_place", round = 1, stage = "knockout", matchCode = "R1-M3", player1Id = "s3", player2Id = "s7", winnerId = "s3", status = "completed"),
      com.example.data.model.TournamentMatch(id = "qf4", tournamentId = "t_ko_place", round = 1, stage = "knockout", matchCode = "R1-M4", player1Id = "s4", player2Id = "s8", winnerId = "s4", status = "completed"),
      com.example.data.model.TournamentMatch(id = "sf1", tournamentId = "t_ko_place", round = 2, stage = "knockout", matchCode = "R2-M1", player1Id = "s1", player2Id = "s3", winnerId = "s1", status = "completed"),
      com.example.data.model.TournamentMatch(id = "sf2", tournamentId = "t_ko_place", round = 2, stage = "knockout", matchCode = "R2-M2", player1Id = "s2", player2Id = "s4", winnerId = "s2", status = "completed"),
      com.example.data.model.TournamentMatch(id = "3rd", tournamentId = "t_ko_place", round = 3, stage = "knockout", matchCode = "R3-3RD", player1Id = "s3", player2Id = "s4", winnerId = "s3", isThirdPlaceMatch = true, status = "completed"),
      com.example.data.model.TournamentMatch(id = "fin", tournamentId = "t_ko_place", round = 3, stage = "knockout", matchCode = "R3-FIN", player1Id = "s1", player2Id = "s2", winnerId = "s1", status = "completed")
    )

    assertTrue(helper.computeStudentTournamentPlacement(koTournament, koMatches, "s1", students).contains("Şampiyon"))
    assertTrue(helper.computeStudentTournamentPlacement(koTournament, koMatches, "s2", students).contains("2. sırada"))
    assertTrue(helper.computeStudentTournamentPlacement(koTournament, koMatches, "s3", students).contains("3. sırada"))
    assertEquals("4. sırada (Yarı Finalist)", helper.computeStudentTournamentPlacement(koTournament, koMatches, "s4", students))
    val qfDesc = helper.computeStudentTournamentPlacement(koTournament, koMatches, "s5", students)
    assertTrue("Expected Çeyrek Finalist in $qfDesc", qfDesc.contains("Çeyrek Finalist"))

    // 2. League tournament ordinal rank + points
    val leagueTournament = com.example.data.model.Tournament(
      id = "t_lig_place",
      classRoomId = "c1",
      name = "Sınıf Ligi",
      type = com.example.data.model.TournamentType.LIG,
      participants = listOf("s1", "s2", "s3"),
      status = com.example.data.model.TournamentStatus.TAMAMLANDI,
      winnerId = "s2"
    )
    val leagueMatches = listOf(
      com.example.data.model.TournamentMatch(id = "lm1", tournamentId = "t_lig_place", round = 1, stage = "group", player1Id = "s2", player2Id = "s1", score1 = 3, score2 = 0, winnerId = "s2", status = "completed"),
      com.example.data.model.TournamentMatch(id = "lm2", tournamentId = "t_lig_place", round = 2, stage = "group", player1Id = "s1", player2Id = "s3", score1 = 2, score2 = 0, winnerId = "s1", status = "completed")
    )
    val s1LeagueRank = helper.computeStudentTournamentPlacement(leagueTournament, leagueMatches, "s1", students)
    assertTrue("Expected 2. sırada and 3 Puan in $s1LeagueRank", s1LeagueRank.contains("2. sırada") && s1LeagueRank.contains("3 Puan"))
  }

  @Test
  fun testAnnouncementIdentityFormattingAndLegacyResolution() {
    val helper = com.example.util.AnnouncementIdentityHelper
    val student = com.example.data.model.Student(
      id = "s45",
      studentNumber = "45",
      fullName = "SEMİH AHMET KAVAKOĞLU",
      gender = "Erkek",
      parentName = "Cihan Öğretmen İLETİŞİM",
      linkedParents = listOf(
        com.example.data.model.LinkedParentAccount(
          email = "cihan@example.com",
          name = "Cihan Öğretmen İLETİŞİM",
          relationship = "Veli"
        )
      )
    )

    val label = helper.formatStudentParentLabel(student, "Cihan Öğretmen İLETİŞİM")
    assertEquals("45 SEMİH AHMET KAVAKOĞLU Velisi (Cihan Öğretmen İLETİŞİM)", label)

    // Existing/legacy raw Google display name should resolve cleanly to student+parent label for teacher display
    val resolvedLegacy = helper.resolveDisplayLabelForTeacher("Cihan Öğretmen İLETİŞİM", listOf(student))
    assertEquals("45 SEMİH AHMET KAVAKOĞLU Velisi (Cihan Öğretmen İLETİŞİM)", resolvedLegacy)

    // Expected recipients list includes student+parent formatting
    val expected = helper.formatExpectedRecipientsForAnnouncement(
      targetType = com.example.data.model.AnnouncementTargetType.ALL_CLASS,
      targetStudentIds = emptyList(),
      allStudents = listOf(student)
    )
    assertEquals(listOf("45 SEMİH AHMET KAVAKOĞLU Velisi (Cihan Öğretmen İLETİŞİM)"), expected)
  }

  @Test
  fun testAnnouncementPollSingleVoteImmutabilityAndReadReceiptMatching() {
    val helper = com.example.util.AnnouncementIdentityHelper
    val student = com.example.data.model.Student(
      id = "s45",
      studentNumber = "45",
      fullName = "SEMİH AHMET KAVAKOĞLU",
      gender = "Erkek",
      parentName = "Cihan Öğretmen İLETİŞİM"
    )
    val voterLabel = helper.formatParentActionLabel(student, listOf(student), "Cihan Öğretmen İLETİŞİM")
    val aliases = helper.voterAliasesForParent(student, listOf(student), "Cihan Öğretmen İLETİŞİM")

    val poll = com.example.data.model.AnnouncementPoll(
      id = "poll_1",
      question = "Geziye katılacak mısınız?",
      options = listOf(
        com.example.data.model.PollOption(id = "opt_yes", text = "Evet"),
        com.example.data.model.PollOption(id = "opt_no", text = "Hayır")
      )
    )

    // First vote succeeds
    val afterFirstVote = helper.applySingleVote(poll, "opt_yes", voterLabel, aliases)
    assertNotNull(afterFirstVote)
    assertEquals(1, afterFirstVote!!.totalVotes)
    assertTrue(helper.hasVotedInPoll(afterFirstVote, aliases))

    // Changing vote to "opt_no" MUST be blocked (returns null)
    val attemptChange = helper.applySingleVote(afterFirstVote, "opt_no", voterLabel, aliases)
    assertNull("Changing answer after voting must not be allowed", attemptChange)

    // Even if voter already voted with legacy raw name, changing/re-voting must be blocked
    val legacyVotedPoll = poll.copy(
      options = listOf(
        com.example.data.model.PollOption(
          id = "opt_yes",
          text = "Evet",
          voterParentNames = listOf("Cihan Öğretmen İLETİŞİM")
        ),
        com.example.data.model.PollOption(id = "opt_no", text = "Hayır")
      )
    )
    assertTrue(helper.hasVotedInPoll(legacyVotedPoll, aliases))
    assertNull(helper.applySingleVote(legacyVotedPoll, "opt_no", voterLabel, aliases))

    // Read receipt matching works for both formatted and legacy parent names
    val ann = com.example.data.model.Announcement(
      id = "ann1",
      classId = "c1",
      title = "Test",
      content = "Test",
      readReceipts = mapOf("Cihan Öğretmen İLETİŞİM" to 123456L)
    )
    assertEquals(123456L, helper.findStudentReadTimestamp(student, ann.readReceipts))
    val summary = helper.resolveAnnouncementReadSummary(ann, listOf(student))
    assertEquals(1 to 1, summary)
  }

  @Test
  fun testFullClassroomStarRankingWithKvkkAnonymization() {
    val allStudents = (1..25).map { idx ->
      com.example.data.model.Student(
        id = "std_$idx",
        studentNumber = "$idx",
        fullName = "ÖĞRENCİ $idx",
        gender = if (idx % 2 == 0) "Kız" else "Erkek"
      )
    }
    val stars = listOf(
      com.example.data.model.StarRewardRecord(
        id = "sr1", classId = "c1", studentId = "std_5", studentName = "ÖĞRENCİ 5",
        studentNumber = "5", categoryId = "math", categoryName = "Matematik", starCount = 48,
        note = "Ders başarısı"
      ),
      com.example.data.model.StarRewardRecord(
        id = "sr2", classId = "c1", studentId = "std_12", studentName = "ÖĞRENCİ 12",
        studentNumber = "12", categoryId = "book", categoryName = "Kitap Kurdu", starCount = 24,
        note = "Kitap okuma"
      ),
      com.example.data.model.StarRewardRecord(
        id = "sr3", classId = "c1", studentId = "std_20", studentName = "ÖĞRENCİ 20",
        studentNumber = "20", categoryId = "tr", categoryName = "Türkçe", starCount = 30,
        note = "Ödev tamamlama"
      )
    )

    val ranked = com.example.util.ParentPrivacyHelper.buildMaskedClassStarRanking(
      allStudents = allStudents,
      allClassStars = stars,
      linkedStudentIds = setOf("std_5", "std_12"),
      activeStudentId = "std_5"
    )

    // All 25 students MUST appear (no 15-student cap!)
    assertEquals(25, ranked.size)
    // Order: std_5 (48), std_20 (30), std_12 (24), then all remaining 0-star students
    assertEquals(1, ranked[0].rank)
    assertEquals("ÖĞRENCİ 5 (Çocuğunuz)", ranked[0].displayName)
    assertEquals(48, ranked[0].starCount)
    assertTrue(ranked[0].isActiveChild)

    // Classmate std_20 MUST be KVKK anonymized to "Öğrenci" while still showing 30 stars!
    assertEquals(2, ranked[1].rank)
    assertEquals("Öğrenci", ranked[1].displayName)
    assertEquals(30, ranked[1].starCount)
    assertFalse(ranked[1].isMyChild)

    // Second linked child std_12 is 3rd
    assertEquals(3, ranked[2].rank)
    assertEquals("ÖĞRENCİ 12 (Çocuğunuz)", ranked[2].displayName)
    assertEquals(24, ranked[2].starCount)

    // 0-star classmates also included and anonymized
    assertTrue(ranked.drop(3).all { it.displayName == "Öğrenci" && it.starCount == 0 })
  }

  @Test
  fun testPollVoteLockedByAccountKeysAcrossNameChange() {
    val helper = com.example.util.AnnouncementIdentityHelper
    val accountKeys = helper.buildVoterAccountKeys(userId = "uid_parent_1", userEmail = "Parent1@Example.com")
    val poll = com.example.data.model.AnnouncementPoll(
      id = "poll_acc",
      question = "Müze gezisi saati?",
      options = listOf(
        com.example.data.model.PollOption(id = "opt_a", text = "Sabah"),
        com.example.data.model.PollOption(id = "opt_b", text = "Öğleden sonra")
      )
    )

    val afterVote = helper.applySingleVote(
      poll = poll,
      optionId = "opt_a",
      voterLabel = "12 ALİ Velisi (Eski İsim)",
      voterAliases = setOf("Eski İsim"),
      voterAccountKeys = accountKeys
    )
    assertNotNull(afterVote)
    assertEquals(1, afterVote!!.totalVotes)
    assertTrue(afterVote.options.first().voterIds.contains("uid:uid_parent_1"))
    assertTrue(afterVote.options.first().voterIds.contains("email:parent1@example.com"))

    // Even if parent changes display name or student aliases don't match, same uid/email MUST be blocked
    assertTrue(helper.hasVotedInPoll(afterVote, setOf("Tamamen Farklı İsim"), accountKeys))
    val attemptReVote = helper.applySingleVote(
      poll = afterVote,
      optionId = "opt_b",
      voterLabel = "99 VELİ Velisi (Tamamen Farklı İsim)",
      voterAliases = setOf("Tamamen Farklı İsim"),
      voterAccountKeys = accountKeys
    )
    assertNull("Changing or re-voting with same account keys must be rejected", attemptReVote)
  }

  @Test
  fun testRolloverVersionNaming101FormatAndComparison() {
    val manager = com.example.util.AppUpdateManager
    assertEquals(1099, manager.extractVersionCode("1.0.99"))
    assertEquals(1100, manager.extractVersionCode("1.0.100"))
    assertEquals(1100, manager.extractVersionCode("1.01.00"))
    assertEquals(1101, manager.extractVersionCode("1.01.01"))
    assertEquals(1205, manager.extractVersionCode("1.02.05"))

    assertTrue(manager.isRemoteNewer("1.01.00", 1100, "1.0.99", 1099))
    assertTrue(manager.isRemoteNewer("1.01.01", 1101, "1.0.100", 1100))
    assertTrue(manager.isRemoteNewer("1.01.01", 1101, "1.01.00", 1100))
    assertFalse(manager.isRemoteNewer("1.01.00", 1100, "1.01.01", 1101))

    val notif101 = com.example.data.model.BirizNotification(
      id = "app_update_v101",
      type = com.example.data.model.NotificationType.APP_UPDATE,
      title = "Yeni Güncelleme Hazır (v1.01.00)",
      message = "BİRİZ v1.01.00 yayında",
      timestamp = 100L,
      isRead = false,
      targetId = null,
      targetSubPageName = "APP_UPDATE"
    )
    assertEquals(1100, com.example.util.NotificationLifecycleHelper.extractUpdateNotificationVersionCode(notif101))
    assertTrue(com.example.util.NotificationLifecycleHelper.compareVersionNames("1.01.00", "1.0.99") > 0)
    assertTrue(com.example.util.NotificationLifecycleHelper.compareVersionNames("1.01.01", "1.01.00") > 0)
  }

  @Test
  fun testTournamentPdfSlotFormattingAndUuidResolution() {
    val helper = com.example.util.TournamentOutcomeHelper
    val students = listOf(
      com.example.data.model.Student(id = "u1", studentNumber = "1", fullName = "Ahmet Yılmaz", gender = "Erkek"),
      com.example.data.model.Student(id = "u2", studentNumber = "2", fullName = "Ayşe Demir", gender = "Kız"),
      com.example.data.model.Student(id = "u3", studentNumber = "3", fullName = "Mehmet Kaya", gender = "Erkek"),
      com.example.data.model.Student(id = "u4", studentNumber = "4", fullName = "Zeynep Ak", gender = "Kız")
    )
    val t = com.example.data.model.Tournament(
      id = "t_ko_slots",
      classRoomId = "c1",
      name = "Satranç",
      type = com.example.data.model.TournamentType.ELEME,
      participants = listOf("u1", "u2", "u3", "u4")
    )

    // Verify UUID -> fullName resolution (prevents UUID showing in league/group podiums)
    val pairs = helper.resolveParticipantPairs(t.participants, emptyList(), students)
    assertEquals("Ahmet Yılmaz", pairs.first { it.first == "u1" }.second)
    assertEquals("Zeynep Ak", pairs.first { it.first == "u4" }.second)

    val koMatches = com.example.util.TournamentEngine.generateKnockoutFixture(
      tournamentId = t.id,
      participants = pairs,
      isDoubleMatch = true
    )

    val finalMatch = koMatches.first { !it.isThirdPlaceMatch && it.round == 2 && !it.matchCode.endsWith("-R") }
    val thirdMatch = koMatches.first { it.isThirdPlaceMatch && !it.matchCode.endsWith("-R") }

    // Unplayed round 2 should show "... Maçının Kazananı" and "... Maçının Kaybedeni"
    assertEquals("R1-M1 Maçının Kazananı", helper.formatMatchSlotLabel(finalMatch, true, koMatches, students))
    assertEquals("R1-M2 Maçının Kazananı", helper.formatMatchSlotLabel(finalMatch, false, koMatches, students))
    assertEquals("R1-M1 Maçının Kaybedeni", helper.formatMatchSlotLabel(thirdMatch, true, koMatches, students))
    assertEquals("R1-M2 Maçının Kaybedeni", helper.formatMatchSlotLabel(thirdMatch, false, koMatches, students))

    // Bracket node columns should combine leg 1 + -R leg and retain source connector codes
    val cols = helper.buildBracketColumns(t, koMatches, students)
    assertEquals(2, cols.size)
    assertEquals(2, cols[1]!!.size)
    val finalNode = cols[2]!!.first { !it.isThirdPlaceMatch }
    assertEquals("R1-M1", finalNode.p1SourceBaseCode)
    assertEquals("R1-M2", finalNode.p2SourceBaseCode)
    assertEquals(2, finalNode.legs.size)
  }

  @Test
  fun testTournamentProjectedPlayoffAndGroupReports() {
    val helper = com.example.util.TournamentOutcomeHelper
    val students = (1..8).map {
      com.example.data.model.Student(id = "s$it", studentNumber = "$it", fullName = "Öğrenci $it", gender = "Kız")
    }
    val tournament = com.example.data.model.Tournament(
      id = "t_ge",
      classRoomId = "c1",
      name = "Grup + Eleme Futbol",
      type = com.example.data.model.TournamentType.GRUP_ELEME,
      participants = students.map { it.id },
      groupCount = 2,
      advancingPerGroup = 2,
      winnerSelectionMethod = com.example.data.model.WinnerSelectionMethod.SCORE
    )
    val pairs = helper.resolveParticipantPairs(tournament.participants, emptyList(), students)
    val groupMatches = com.example.util.TournamentEngine.generateGroupFixtures(
      tournamentId = tournament.id,
      participants = pairs,
      groupCount = 2,
      groupNaming = "letters",
      isDoubleMatch = false
    ).mapIndexed { index, m ->
      if (index == 0) m.copy(status = "completed", score1 = 3, score2 = 1, winnerId = m.player1Id) else m
    }

    val reports = helper.buildGroupReports(tournament, groupMatches, students)
    assertEquals(2, reports.size)
    assertEquals(1, reports.sumOf { it.completedCount })
    assertTrue(reports[0].lastCompletedMatchText.contains("3-1"))

    // Even when Play-Off hasn't been generated yet, projected Play-Off tree produces full Day-1 bracket
    val projected = helper.buildProjectedPlayoffMatches(tournament, reports, emptyList())
    assertTrue(projected.isNotEmpty())
    val projectedCols = helper.buildBracketColumns(tournament, projected, students)
    assertEquals(2, projectedCols.size) // Semi-Final (KO-R1-M1, KO-R1-M2) + Final & 3rd Place (R2)
    val semi1 = projectedCols[1]!!.first()
    assertTrue(semi1.player1Label.contains("1.si"))
    assertTrue(semi1.player2Label.contains("2.si"))
    val finalNode = projectedCols[2]!!.first { !it.isThirdPlaceMatch }
    assertEquals("KO-R1-M1 Maçının Kazananı", finalNode.player1Label)
    assertEquals("KO-R1-M2 Maçının Kazananı", finalNode.player2Label)
  }

  @Test
  fun testParentChildTournamentOutcomesAllTypes() {
    val helper = com.example.util.TournamentOutcomeHelper
    val students = (1..4).map {
      com.example.data.model.Student(id = "c$it", studentNumber = "$it", fullName = "Çocuk $it", gender = "Erkek")
    }
    val stars = com.example.data.model.TournamentStarRewards(
      enabled = true,
      firstPlaceStars = 15,
      secondPlaceStars = 10,
      thirdPlaceStars = 5,
      participationStars = 2
    )

    // 1. Completed League Tournament
    val leagueTournament = com.example.data.model.Tournament(
      id = "t_lig",
      classRoomId = "cl1",
      name = "Sınıf Ligi",
      type = com.example.data.model.TournamentType.LIG,
      status = com.example.data.model.TournamentStatus.TAMAMLANDI,
      participants = listOf("c1", "c2", "c3"),
      winnerSelectionMethod = com.example.data.model.WinnerSelectionMethod.SCORE,
      starRewards = stars
    )
    val ligMatches = listOf(
      com.example.data.model.TournamentMatch(
        id = "lm1", tournamentId = "t_lig", round = 1, matchCode = "L1-M1",
        player1Id = "c1", player1Name = "Çocuk 1", player2Id = "c2", player2Name = "Çocuk 2",
        score1 = 2, score2 = 0, winnerId = "c1", status = "completed", stage = "league"
      ),
      com.example.data.model.TournamentMatch(
        id = "lm2", tournamentId = "t_lig", round = 2, matchCode = "L2-M1",
        player1Id = "c1", player1Name = "Çocuk 1", player2Id = "c3", player2Name = "Çocuk 3",
        score1 = 3, score2 = 1, winnerId = "c1", status = "completed", stage = "league"
      )
    )
    val c1LeagueOutcome = helper.evaluateChildTournamentOutcome(leagueTournament, ligMatches, students, "c1")
    assertTrue(c1LeagueOutcome.isParticipant)
    assertEquals(1, c1LeagueOutcome.overallRank)
    assertEquals(15, c1LeagueOutcome.earnedStars)
    assertTrue(c1LeagueOutcome.headline.contains("Şampiyonu"))

    val c4NonParticipant = helper.evaluateChildTournamentOutcome(leagueTournament, ligMatches, students, "c4")
    assertFalse(c4NonParticipant.isParticipant)

    // 2. Completed Elimination Tournament (Champion, Runner-Up, 3rd, 4th)
    val koTournament = com.example.data.model.Tournament(
      id = "t_eleme",
      classRoomId = "cl1",
      name = "Bilgi Eleme",
      type = com.example.data.model.TournamentType.ELEME,
      status = com.example.data.model.TournamentStatus.TAMAMLANDI,
      participants = listOf("c1", "c2", "c3", "c4"),
      winnerId = "c1",
      runnerUpId = "c2",
      thirdPlaceId = "c3",
      starRewards = stars
    )
    val koMatches = listOf(
      com.example.data.model.TournamentMatch(
        id = "km1", tournamentId = "t_eleme", round = 1, matchCode = "R1-M1",
        player1Id = "c1", player2Id = "c3", winnerId = "c1", status = "completed", stage = "knockout"
      ),
      com.example.data.model.TournamentMatch(
        id = "km2", tournamentId = "t_eleme", round = 1, matchCode = "R1-M2",
        player1Id = "c2", player2Id = "c4", winnerId = "c2", status = "completed", stage = "knockout"
      ),
      com.example.data.model.TournamentMatch(
        id = "km3", tournamentId = "t_eleme", round = 2, matchCode = "R2-M-3RD",
        player1Id = "c3", player2Id = "c4", winnerId = "c3", isThirdPlaceMatch = true, status = "completed", stage = "knockout"
      ),
      com.example.data.model.TournamentMatch(
        id = "km4", tournamentId = "t_eleme", round = 2, matchCode = "R2-M1",
        player1Id = "c1", player2Id = "c2", winnerId = "c1", status = "completed", stage = "knockout"
      )
    )
    assertEquals(15, helper.evaluateChildTournamentOutcome(koTournament, koMatches, students, "c1").earnedStars)
    assertEquals(10, helper.evaluateChildTournamentOutcome(koTournament, koMatches, students, "c2").earnedStars)
    assertEquals(5, helper.evaluateChildTournamentOutcome(koTournament, koMatches, students, "c3").earnedStars)
    val c4FourthOutcome = helper.evaluateChildTournamentOutcome(koTournament, koMatches, students, "c4")
    assertEquals(4, c4FourthOutcome.overallRank)
    assertEquals(2, c4FourthOutcome.earnedStars)
    assertTrue(c4FourthOutcome.headline.contains("4."))

    // 3. Group + Elimination: eliminated in group stage
    val geTournament = com.example.data.model.Tournament(
      id = "t_ge_end",
      classRoomId = "cl1",
      name = "Grup+Eleme",
      type = com.example.data.model.TournamentType.GRUP_ELEME,
      status = com.example.data.model.TournamentStatus.TAMAMLANDI,
      participants = listOf("c1", "c2", "c3", "c4"),
      advancingPerGroup = 1,
      starRewards = stars
    )
    val geMatches = listOf(
      com.example.data.model.TournamentMatch(
        id = "gm1", tournamentId = "t_ge_end", round = 1, matchCode = "G1-M1",
        player1Id = "c1", player2Id = "c2", score1 = 3, score2 = 0, winnerId = "c1",
        groupId = "A", groupName = "A Grubu", status = "completed", stage = "group"
      ),
      com.example.data.model.TournamentMatch(
        id = "gm2", tournamentId = "t_ge_end", round = 1, matchCode = "G2-M1",
        player1Id = "c3", player2Id = "c4", score1 = 2, score2 = 1, winnerId = "c3",
        groupId = "B", groupName = "B Grubu", status = "completed", stage = "group"
      ),
      com.example.data.model.TournamentMatch(
        id = "gko1", tournamentId = "t_ge_end", round = 1, matchCode = "KO-R1-M1",
        player1Id = "c1", player2Id = "c3", score1 = 2, score2 = 1, winnerId = "c1",
        status = "completed", stage = "knockout"
      )
    )
    val c2ElimOutcome = helper.evaluateChildTournamentOutcome(geTournament, geMatches, students, "c2")
    assertEquals("Grup Aşaması", c2ElimOutcome.stageReached)
    assertTrue(c2ElimOutcome.headline.contains("Grup Aşamasında Elendi"))
    assertEquals(2, c2ElimOutcome.earnedStars)

    val c1ChampionOutcome = helper.evaluateChildTournamentOutcome(geTournament, geMatches, students, "c1")
    assertEquals(1, c1ChampionOutcome.overallRank)
    assertEquals(15, c1ChampionOutcome.earnedStars)
    assertTrue(c1ChampionOutcome.headline.contains("A Grubu"))
  }

  @Test
  fun testBackgroundUpdateApkCachePreservationAndVersionParsing() {
    assertEquals(1105, com.example.util.AppUpdateManager.parseVersionCodeFromApkFileName("Biriz_v1105.apk"))
    assertEquals(1105, com.example.util.AppUpdateManager.parseVersionCodeFromApkFileName("Biriz_v1105.apk.tmp"))
    assertNull(com.example.util.AppUpdateManager.parseVersionCodeFromApkFileName("Biriz_latest.apk"))
    assertNull(com.example.util.AppUpdateManager.parseVersionCodeFromApkFileName("notes.txt"))

    val newerApk = java.io.File("/tmp/Biriz_v1105.apk")
    val olderApk = java.io.File("/tmp/Biriz_v1100.apk")
    val legacyApk = java.io.File("/tmp/Biriz_latest.apk")
    val activeTmp = java.io.File("/tmp/Biriz_v1105.apk.tmp")

    // Newer cached APK must be preserved across app restarts (current = 1100)
    assertTrue(
      com.example.util.AppUpdateManager.shouldPreserveApkFile(
        file = newerApk,
        currentVersionCode = 1100,
        keepFile = null,
        activeKeepPath = null,
        isDownloading = false
      )
    )
    // Already-installed or older cached APK must be cleaned
    assertFalse(
      com.example.util.AppUpdateManager.shouldPreserveApkFile(
        file = olderApk,
        currentVersionCode = 1100,
        keepFile = null,
        activeKeepPath = null,
        isDownloading = false
      )
    )
    assertFalse(
      com.example.util.AppUpdateManager.shouldPreserveApkFile(
        file = legacyApk,
        currentVersionCode = 1100,
        keepFile = null,
        activeKeepPath = null,
        isDownloading = false
      )
    )
    // Active temp file is preserved while downloading, and cleaned up when idle
    assertTrue(
      com.example.util.AppUpdateManager.shouldPreserveApkFile(
        file = activeTmp,
        currentVersionCode = 1100,
        keepFile = null,
        activeKeepPath = null,
        isDownloading = true
      )
    )
    assertFalse(
      com.example.util.AppUpdateManager.shouldPreserveApkFile(
        file = activeTmp,
        currentVersionCode = 1100,
        keepFile = null,
        activeKeepPath = null,
        isDownloading = false
      )
    )
  }

  @Test
  fun testBackgroundUpdateDownloadControllerAndChannels() {
    assertEquals("biriz_update_download_progress", com.example.util.AppUpdateNotificationHelper.CHANNEL_DOWNLOAD_ID)
    assertEquals(2027, com.example.util.AppUpdateNotificationHelper.DOWNLOAD_NOTIFICATION_ID)
    assertEquals("extra_install_ready_apk", com.example.util.AppUpdateNotificationHelper.EXTRA_INSTALL_READY_APK)

    assertEquals("0.0 MB", com.example.util.AppUpdateDownloadController.formatBytesMb(0L))
    val onePointFiveMb = (1.5 * 1024 * 1024).toLong()
    assertEquals("1,5 MB", com.example.util.AppUpdateDownloadController.formatBytesMb(onePointFiveMb))

    val info = com.example.util.UpdateInfo(
      isUpdateAvailable = true,
      currentVersionName = "1.01.00",
      currentVersionCode = 1100,
      latestVersionName = "1.01.05",
      latestVersionCode = 1105,
      downloadUrl = "https://github.com/CHNZL/biriz/releases/download/v1.01.05/Biriz.apk",
      releaseNotes = "Arka plan güncelleme desteği"
    )
    com.example.util.AppUpdateDownloadController.updateProgress(info, 0.5f, 5_000_000L, 10_000_000L)
    assertTrue(com.example.util.AppUpdateDownloadController.isActivelyDownloading())

    com.example.util.AppUpdateDownloadController.markFailed(info, "Ağ zaman aşımı")
    assertFalse(com.example.util.AppUpdateDownloadController.isActivelyDownloading())
    assertTrue(com.example.util.AppUpdateDownloadController.state.value is com.example.util.BackgroundUpdateState.Failed)

    com.example.util.AppUpdateDownloadController.markIdle()
    assertTrue(com.example.util.AppUpdateDownloadController.state.value is com.example.util.BackgroundUpdateState.Idle)
  }
}




