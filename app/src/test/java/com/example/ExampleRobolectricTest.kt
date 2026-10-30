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
@Config(sdk = [36])
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
}
