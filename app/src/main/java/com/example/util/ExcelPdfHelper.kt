package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.R
import com.example.data.model.ClassRoom
import com.example.data.model.DeskPosition
import com.example.data.model.DoorPosition
import com.example.data.model.GroupTeam
import com.example.data.model.ScheduleConfig
import com.example.data.model.ScheduleLesson
import com.example.data.model.ScheduleTimelineItem
import com.example.data.model.SeatingConfig
import com.example.data.model.SeatingPlanState
import com.example.data.model.Student
import com.example.data.model.WindowPosition
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object ExcelPdfHelper {

    /**
     * BİRİZ kurumsal kimlik amblemini (öğretmen-veli-öğrenci sıcak sarmal logo)
     * PDF Canvas üzerine yüksek çözünürlükte basılmak üzere Bitmap olarak çizer.
     */
    private fun renderEmblemBitmap(context: Context, sizePx: Int = 160): Bitmap? {
        return runCatching {
            val drawable = ContextCompat.getDrawable(context, R.drawable.ic_biriz_emblem) ?: return null
            val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, sizePx, sizePx)
            drawable.draw(canvas)
            bitmap
        }.getOrNull()
    }

    private fun escapeXml(text: String): String {
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    private fun unescapeXml(text: String): String {
        return text.replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&amp;", "&")
    }

    private fun colLetter(colIdx: Int): String {
        var n = colIdx
        var str = ""
        while (n >= 0) {
            str = ('A'.code + (n % 26)).toChar() + str
            n = (n / 26) - 1
        }
        return str
    }

    /**
     * Microsoft Excel (.xlsx - Office Open XML) standardına tam uyumlu
     * gerçek bir Excel çalışma kitabı dosyası üretir.
     */
    private fun writeXlsx(file: File, headers: List<String>, rows: List<List<String>>) {
        ZipOutputStream(FileOutputStream(file)).use { zos ->
            // 1. [Content_Types].xml
            zos.putNextEntry(ZipEntry("[Content_Types].xml"))
            val contentTypes = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
<Default Extension="xml" ContentType="application/xml"/>
<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
<Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
</Types>""".trimIndent()
            zos.write(contentTypes.toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            // 2. _rels/.rels
            zos.putNextEntry(ZipEntry("_rels/.rels"))
            val rels = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>""".trimIndent()
            zos.write(rels.toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            // 3. xl/workbook.xml
            zos.putNextEntry(ZipEntry("xl/workbook.xml"))
            val wb = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
<sheets><sheet name="Öğrenci Listesi" sheetId="1" r:id="rId1"/></sheets>
</workbook>""".trimIndent()
            zos.write(wb.toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            // 4. xl/_rels/workbook.xml.rels
            zos.putNextEntry(ZipEntry("xl/_rels/workbook.xml.rels"))
            val wbRels = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
</Relationships>""".trimIndent()
            zos.write(wbRels.toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            // 5. xl/worksheets/sheet1.xml
            zos.putNextEntry(ZipEntry("xl/worksheets/sheet1.xml"))
            val sheetBuilder = StringBuilder()
            sheetBuilder.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
<sheetData>
""")

            // Başlık Satırı (1. Satır)
            var rowNum = 1
            sheetBuilder.append("<row r=\"$rowNum\">")
            headers.forEachIndexed { colIdx, header ->
                val ref = "${colLetter(colIdx)}$rowNum"
                sheetBuilder.append("<c r=\"$ref\" t=\"inlineStr\"><is><t>${escapeXml(header)}</t></is></c>")
            }
            sheetBuilder.append("</row>\n")

            // Veri Satırları
            rows.forEach { rowData ->
                rowNum++
                sheetBuilder.append("<row r=\"$rowNum\">")
                rowData.forEachIndexed { colIdx, value ->
                    val ref = "${colLetter(colIdx)}$rowNum"
                    sheetBuilder.append("<c r=\"$ref\" t=\"inlineStr\"><is><t>${escapeXml(value)}</t></is></c>")
                }
                sheetBuilder.append("</row>\n")
            }

            sheetBuilder.append("</sheetData>\n</worksheet>")
            zos.write(sheetBuilder.toString().toByteArray(Charsets.UTF_8))
            zos.closeEntry()
        }
    }

    /**
     * Örnek Öğrenci Excel Şablonu (.xlsx) üretir.
     * Sütunlar:
     * 1. Sütun: Okul No
     * 2. Sütun: Adı
     * 3. Sütun: Soyadı
     * 4. Sütun: Cinsiyet
     */
    fun createSampleTemplateFile(context: Context): Uri {
        val fileName = "biriz_ogrenci_ekleme_sablonu.xlsx"
        val file = File(context.cacheDir, fileName)

        val headers = listOf("Okul No", "Adı", "Soyadı", "Cinsiyet")
        val sampleRows = listOf(
            listOf("101", "AHMET", "YILMAZ", "Erkek"),
            listOf("102", "AYŞE", "KAYA", "Kız"),
            listOf("103", "MEHMET", "DEMİR", "Erkek"),
            listOf("104", "ZEYNEP", "ÇELİK", "Kız")
        )

        writeXlsx(file, headers, sampleRows)

        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    /**
     * Yüklenen dosyadan öğrencileri ayrıştırır.
     * Hem gerçek Excel (.xlsx) dosyalarını hem de CSV (.csv / metin) dosyalarını
     * otomatik algılayıp çözer.
     * Sütun formatı:
     * 1. Sütun: Okul No
     * 2. Sütun: Adı
     * 3. Sütun: Soyadı
     * 4. Sütun: Cinsiyet
     * (Eski 3 sütunlu "Okul No;Adı Soyadı;Cinsiyet" yapısıyla da geriye dönük uyumludur).
     */
    fun parseStudentsFile(context: Context, uri: Uri): Result<List<Triple<Int, String, String>>> {
        return runCatching {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: throw IllegalArgumentException("Dosya açılamadı.")
            val rawBytes = inputStream.readBytes()
            inputStream.close()

            if (rawBytes.isEmpty()) {
                throw IllegalArgumentException("Seçilen dosya boş.")
            }

            // Dosya tipini belirle: ZIP header (0x50, 0x4B, 0x03, 0x04) -> XLSX
            val isXlsx = rawBytes.size >= 4 &&
                    rawBytes[0] == 0x50.toByte() &&
                    rawBytes[1] == 0x4B.toByte() &&
                    rawBytes[2] == 0x03.toByte() &&
                    rawBytes[3] == 0x04.toByte()

            val rowsData = if (isXlsx) {
                parseXlsxBytes(rawBytes)
            } else {
                parseCsvBytes(rawBytes)
            }

            val resultList = mutableListOf<Triple<Int, String, String>>()
            val seenNumbers = mutableSetOf<Int>()

            for ((index, tokens) in rowsData.withIndex()) {
                if (tokens.isEmpty()) continue

                // Başlık satırı kontrolü (İlk satırda Okul / No / Adı / Soyadı varsa atla)
                if (index == 0 && tokens.any { token ->
                        token.contains("Okul", ignoreCase = true) ||
                        token.contains("No", ignoreCase = true) ||
                        token.contains("Adı", ignoreCase = true) ||
                        token.contains("Soyad", ignoreCase = true)
                    }) {
                    continue
                }

                if (tokens.size >= 2) {
                    val no = tokens[0].filter { it.isDigit() }.toIntOrNull()
                    if (no == null || no <= 0) continue

                    val fullName: String
                    val rawGender: String

                    if (tokens.size >= 4) {
                        // 4 Sütunlu Standart Format: 1. Okul No, 2. Adı, 3. Soyadı, 4. Cinsiyet
                        val firstName = TurkishTextHelper.toTurkishUpperCase(tokens[1])
                        val lastName = TurkishTextHelper.toTurkishUpperCase(tokens[2])
                        fullName = "$firstName $lastName".trim()
                        rawGender = tokens[3]
                    } else if (tokens.size == 3) {
                        // 3 Sütunlu Format: 1. Okul No, 2. Adı Soyadı, 3. Cinsiyet
                        fullName = TurkishTextHelper.toTurkishUpperCase(tokens[1])
                        rawGender = tokens[2]
                    } else {
                        fullName = TurkishTextHelper.toTurkishUpperCase(tokens[1])
                        rawGender = "Erkek"
                    }

                    if (fullName.length < 2) continue

                    val gender = if (rawGender.contains("Kız", ignoreCase = true) ||
                        rawGender.contains("Kadin", ignoreCase = true) ||
                        rawGender.equals("K", ignoreCase = true)) {
                        "Kız"
                    } else {
                        "Erkek"
                    }

                    if (!seenNumbers.contains(no)) {
                        seenNumbers.add(no)
                        resultList.add(Triple(no, fullName, gender))
                    }
                }
            }

            if (resultList.isEmpty()) {
                throw IllegalArgumentException("Dosyada geçerli formatta öğrenci bulunamadı. Lütfen 'Örnek Şablon (.xlsx)' yapısını kullanınız.")
            }

            resultList
        }
    }

    private fun parseXlsxBytes(bytes: ByteArray): List<List<String>> {
        val sharedStrings = mutableListOf<String>()
        var sheetXml: String? = null

        ZipInputStream(ByteArrayInputStream(bytes)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (entry.name.contains("sharedStrings.xml")) {
                    val s = zis.readBytes().toString(Charsets.UTF_8)
                    val regex = Regex("<t(?:\\s+[^>]*)?>(.*?)</t>", RegexOption.DOT_MATCHES_ALL)
                    regex.findAll(s).forEach { match ->
                        sharedStrings.add(unescapeXml(match.groupValues[1]))
                    }
                } else if (entry.name.matches(Regex(".*/worksheets/sheet.*\\.xml"))) {
                    sheetXml = zis.readBytes().toString(Charsets.UTF_8)
                }
                entry = zis.nextEntry
            }
        }

        if (sheetXml.isNullOrBlank()) return emptyList()

        val parsedRows = mutableListOf<List<String>>()
        val rowRegex = Regex("<row(?:\\s+[^>]*)?>(.*?)</row>", RegexOption.DOT_MATCHES_ALL)
        val cellRegex = Regex("<c(\\s+[^>]*)?>(.*?)</c>", RegexOption.DOT_MATCHES_ALL)

        for (rowMatch in rowRegex.findAll(sheetXml)) {
            val rowContent = rowMatch.groupValues[1]
            val rowValues = mutableListOf<String>()

            for (cellMatch in cellRegex.findAll(rowContent)) {
                val cellAttrs = cellMatch.groupValues[1]
                val cellInner = cellMatch.groupValues[2]

                var cellValue = ""
                if (cellAttrs.contains("t=\"s\"")) {
                    // Shared string index
                    val vMatch = Regex("<v>(\\d+)</v>").find(cellInner)
                    val sIdx = vMatch?.groupValues?.get(1)?.toIntOrNull()
                    if (sIdx != null && sIdx in sharedStrings.indices) {
                        cellValue = sharedStrings[sIdx]
                    }
                } else {
                    // Inline string or direct value
                    val tMatch = Regex("<t(?:\\s+[^>]*)?>(.*?)</t>", RegexOption.DOT_MATCHES_ALL).find(cellInner)
                    if (tMatch != null) {
                        cellValue = unescapeXml(tMatch.groupValues[1])
                    } else {
                        val vMatch = Regex("<v>(.*?)</v>").find(cellInner)
                        if (vMatch != null) {
                            cellValue = unescapeXml(vMatch.groupValues[1])
                        }
                    }
                }
                rowValues.add(cellValue.trim())
            }

            if (rowValues.any { it.isNotBlank() }) {
                parsedRows.add(rowValues)
            }
        }

        return parsedRows
    }

    private fun parseCsvBytes(bytes: ByteArray): List<List<String>> {
        val reader = BufferedReader(InputStreamReader(ByteArrayInputStream(bytes), Charsets.UTF_8))
        val lines = reader.readLines()
        reader.close()

        val rows = mutableListOf<List<String>>()
        for (rawLine in lines) {
            val line = rawLine.trim().removePrefix("\uFEFF")
            if (line.isBlank()) continue

            val delimiter = when {
                line.contains(";") -> ";"
                line.contains("\t") -> "\t"
                else -> ","
            }

            val tokens = line.split(delimiter).map { it.trim().removeSurrounding("\"") }
            rows.add(tokens)
        }
        return rows
    }

    /**
     * Sınıf Listesini gerçek Microsoft Excel (.xlsx) çalışma kitabı olarak dışa aktarır.
     * Sütunlar:
     * 1. Sütun: Okul No
     * 2. Sütun: Adı
     * 3. Sütun: Soyadı
     * 4. Sütun: Cinsiyet
     * 5. Sütun: Veli Katılım Kodu
     */
    fun exportStudentsToExcel(context: Context, classRoom: ClassRoom, students: List<Student>): Uri {
        val safeSchool = classRoom.schoolName.replace(" ", "_").filter { it.isLetterOrDigit() || it == '_' }
        val fileName = "${safeSchool}_${classRoom.gradeLevel}${classRoom.section}_Sinif_Listesi.xlsx"
        val file = File(context.cacheDir, fileName)

        val headers = listOf("Okul No", "Adı", "Soyadı", "Cinsiyet", "Veli Katılım Kodu")
        val dataRows = students.sortedBy { it.studentNumber.toIntOrNull() ?: 9999 }.map { s ->
            val nameParts = s.fullName.trim().split(" ")
            val firstName = if (nameParts.size > 1) nameParts.dropLast(1).joinToString(" ") else nameParts.firstOrNull() ?: ""
            val lastName = if (nameParts.size > 1) nameParts.last() else ""
            listOf(s.studentNumber, firstName, lastName, s.gender, s.parentInviteCode)
        }

        writeXlsx(file, headers, dataRows)

        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    /**
     * Sınıf Listesini BİRİZ Kurumsal Kimliğine uygun A4 PDF belgesi olarak üretir.
     * - Tablo: Sıra | Okul No | Adı | Soyadı | Cinsiyet | Veli Katılım Kodu
     */
    fun exportStudentsToPdf(context: Context, classRoom: ClassRoom, students: List<Student>): Uri {
        val pdfDocument = PdfDocument()

        val pageWidth = 595
        val pageHeight = 842
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val navyPrimary = Color.parseColor("#13294B")
        val turquoise = Color.parseColor("#28B8C2")
        val amber = Color.parseColor("#D97706")
        val tableHeaderBg = Color.parseColor("#1E3A8A")
        val zebraRowBg = Color.parseColor("#F8FAFC")
        val borderColor = Color.parseColor("#CBD5E1")
        val darkText = Color.parseColor("#0F172A")
        val mutedText = Color.parseColor("#475569")

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val emblemBitmap = renderEmblemBitmap(context, 160)

        // 1. Üst Başlık Şeridi (Mürekkep Dostu / Açık Zeminli Tasarım)
        paint.color = Color.WHITE
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), 95f, paint)

        // Altına zarif sınır çizgisi ve turkuaz kurumsal vurgu çizgisi
        paint.color = borderColor
        paint.strokeWidth = 1f
        canvas.drawLine(0f, 93.5f, pageWidth.toFloat(), 93.5f, paint)
        paint.color = turquoise
        paint.strokeWidth = 2.5f
        canvas.drawLine(0f, 95f, pageWidth.toFloat(), 95f, paint)
        paint.strokeWidth = 1f

        // BİRİZ Resmi Logo Amblemi
        var textStartX = 36f
        if (emblemBitmap != null) {
            val emblemRect = RectF(36f, 16f, 36f + 62f, 16f + 62f)
            canvas.drawBitmap(emblemBitmap, null, emblemRect, paint)
            textStartX = 110f
        }

        // BİRİZ Kurumsal Başlık Metinleri (3 Ayrı ve Ferah Satır, Asla Üst Üste Binmez)
        // 1. Satır: Ana Marka
        paint.color = navyPrimary
        paint.textSize = 22f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("BİRİZ", textStartX, 38f, paint)

        // 2. Satır: Platform Tanımı
        paint.color = Color.parseColor("#0D9488")
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("AKILLI SINIF VE EĞİTİM PLATFORMU", textStartX, 54f, paint)

        // 3. Satır: Belge / Modül Başlığı
        paint.color = mutedText
        paint.textSize = 9.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Sınıf Yönetimi • Resmi Öğrenci ve Veli Katılım Listesi", textStartX, 70f, paint)

        // 2. Sınıf & Okul Bilgi Paneli
        val infoRect = RectF(36f, 110f, (pageWidth - 36).toFloat(), 180f)
        paint.color = Color.parseColor("#F8FAFC")
        canvas.drawRoundRect(infoRect, 10f, 10f, paint)

        paint.style = Paint.Style.STROKE
        paint.color = borderColor
        paint.strokeWidth = 1f
        canvas.drawRoundRect(infoRect, 10f, 10f, paint)
        paint.style = Paint.Style.FILL

        paint.color = darkText
        paint.textSize = 13f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Okul: ${classRoom.schoolName}", 50f, 134f, paint)
        canvas.drawText("Sınıf / Şube: ${classRoom.gradeLevel}/${classRoom.section} Şubesi", 50f, 154f, paint)

        canvas.drawText("Sınıf Öğretmeni: ${classRoom.teacherName}", 320f, 134f, paint)
        val dateStr = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("tr", "TR")).format(Date())
        paint.color = mutedText
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 11f
        canvas.drawText("Oluşturulma Tarihi: $dateStr", 320f, 154f, paint)
        canvas.drawText("Toplam Kayıtlı Öğrenci: ${students.size}", 50f, 172f, paint)

        // 3. Tablo Başlıkları (Mürekkep Dostu Açık Zemin)
        var currentY = 200f
        val tableLeft = 36f
        val tableRight = (pageWidth - 36).toFloat()
        val rowHeight = 24f

        val colX = floatArrayOf(
            tableLeft,            // Sıra No
            tableLeft + 38f,      // Okul No
            tableLeft + 92f,      // Adı
            tableLeft + 225f,     // Soyadı
            tableLeft + 335f,     // Cinsiyet
            tableLeft + 405f,     // Veli Katılım Kodu
            tableRight
        )

        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#F1F5F9")
        canvas.drawRect(tableLeft, currentY, tableRight, currentY + rowHeight, paint)

        paint.style = Paint.Style.STROKE
        paint.color = borderColor
        paint.strokeWidth = 1f
        canvas.drawRect(tableLeft, currentY, tableRight, currentY + rowHeight, paint)
        paint.style = Paint.Style.FILL

        paint.color = navyPrimary
        paint.textSize = 11f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Sıra", colX[0] + 8f, currentY + 16f, paint)
        canvas.drawText("Okul No", colX[1] + 8f, currentY + 16f, paint)
        canvas.drawText("Adı", colX[2] + 8f, currentY + 16f, paint)
        canvas.drawText("Soyadı", colX[3] + 8f, currentY + 16f, paint)
        canvas.drawText("Cinsiyet", colX[4] + 8f, currentY + 16f, paint)
        canvas.drawText("Veli Katılım Kodu", colX[5] + 8f, currentY + 16f, paint)

        currentY += rowHeight

        // 4. Öğrenci Satırları
        val sortedStudents = students.sortedBy { it.studentNumber.toIntOrNull() ?: 9999 }

        sortedStudents.forEachIndexed { idx, student ->
            if (currentY > pageHeight - 60f) {
                return@forEachIndexed
            }

            paint.color = if (idx % 2 == 0) Color.WHITE else zebraRowBg
            canvas.drawRect(tableLeft, currentY, tableRight, currentY + rowHeight, paint)

            paint.color = borderColor
            paint.strokeWidth = 0.5f
            paint.style = Paint.Style.STROKE
            canvas.drawLine(tableLeft, currentY + rowHeight, tableRight, currentY + rowHeight, paint)
            paint.style = Paint.Style.FILL

            val nameParts = student.fullName.trim().split(" ")
            val firstName = if (nameParts.size > 1) nameParts.dropLast(1).joinToString(" ") else nameParts.firstOrNull() ?: ""
            val lastName = if (nameParts.size > 1) nameParts.last() else ""

            paint.textSize = 10f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.color = mutedText
            canvas.drawText("${idx + 1}", colX[0] + 10f, currentY + 16f, paint)

            paint.color = darkText
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(student.studentNumber, colX[1] + 10f, currentY + 16f, paint)

            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(firstName, colX[2] + 8f, currentY + 16f, paint)
            canvas.drawText(lastName, colX[3] + 8f, currentY + 16f, paint)

            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.color = if (student.gender.equals("Kız", ignoreCase = true)) Color.parseColor("#BE185D") else Color.parseColor("#0284C7")
            canvas.drawText(student.gender, colX[4] + 10f, currentY + 16f, paint)

            // Veli Kodu (Kehribar Rozet)
            val codeBadgeRect = RectF(colX[5] + 6f, currentY + 4f, colX[5] + 105f, currentY + 20f)
            paint.color = Color.parseColor("#FEF3C7")
            canvas.drawRoundRect(codeBadgeRect, 6f, 6f, paint)

            paint.color = amber
            paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            paint.textSize = 10f
            canvas.drawText(student.parentInviteCode, colX[5] + 14f, currentY + 15.5f, paint)

            currentY += rowHeight
        }

        paint.color = borderColor
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawRect(tableLeft, 200f, tableRight, currentY, paint)
        paint.style = Paint.Style.FILL

        // 5. Alt Bilgi
        paint.color = mutedText
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText(
            "Bu liste BİRİZ Akıllı Eğitim Platformu tarafından güvenli olarak üretilmiştir. Her öğrencinin veli kodu tek ve benzersizdir.",
            tableLeft,
            (pageHeight - 30).toFloat(),
            paint
        )

        pdfDocument.finishPage(page)

        val safeSchool = classRoom.schoolName.replace(" ", "_").filter { it.isLetterOrDigit() || it == '_' }
        val fileName = "${safeSchool}_${classRoom.gradeLevel}${classRoom.section}_Sinif_Listesi.pdf"
        val file = File(context.cacheDir, fileName)

        FileOutputStream(file).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    /**
     * Öğrencilere ve velilere kesilip dağıtılmak üzere tasarlanmış A4 "Veli Katılım Kartları / Kuponları" PDF belgesi üretir.
     * Her A4 sayfada 8 adet (2 sütun x 4 satır) kesim çizgili kupon yer alır.
     * BİRİZ kurumsal kimliği, logo, okul, sınıf, öğrenci bilgileri, 8 haneli benzersiz Veli Katılım Kodu ve veli yönergeleri içerir.
     */
    fun exportParentInviteCardsPdf(context: Context, classRoom: ClassRoom, students: List<Student>): Uri {
        val pdfDocument = PdfDocument()

        val pageWidth = 595
        val pageHeight = 842

        val cardsPerPage = 8
        val sortedStudents = students.sortedBy { it.studentNumber.toIntOrNull() ?: 9999 }
        val totalPages = maxOf(1, (sortedStudents.size + cardsPerPage - 1) / cardsPerPage)

        val cardWidth = 262f
        val cardHeight = 186f
        val startX = 28f
        val startY = 32f
        val gapX = 15f
        val gapY = 12f

        val navyPrimary = Color.parseColor("#13294B")
        val amberBg = Color.parseColor("#FEF3C7")
        val amberBorder = Color.parseColor("#D97706")
        val amberText = Color.parseColor("#92400E")
        val darkText = Color.parseColor("#0F172A")
        val mutedText = Color.parseColor("#475569")
        val cutLineColor = Color.parseColor("#94A3B8")
        val studentBoxBg = Color.parseColor("#F1F5F9")
        val studentBoxBorder = Color.parseColor("#CBD5E1")

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val emblemBitmap = renderEmblemBitmap(context, 120)
        val qrBitmap = QrCodeHelper.generateQrBitmap(
            content = QrCodeHelper.LATEST_APK_DOWNLOAD_URL,
            size = 250,
            foregroundColor = Color.parseColor("#13294B")
        )
        val dashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1f
            color = cutLineColor
            pathEffect = DashPathEffect(floatArrayOf(5f, 4f), 0f)
        }

        for (pageIndex in 0 until totalPages) {
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex + 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            // Sayfa Üst Bilgisi
            paint.color = navyPrimary
            paint.textSize = 8.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("BİRİZ • Veli Katılım Kartları ve Kuponları (Kesilebilir Çıktı)", startX, 20f, paint)

            paint.color = mutedText
            paint.textSize = 8f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            val pageStr = "Sayfa ${pageIndex + 1} / $totalPages • ${classRoom.schoolName} - ${classRoom.gradeLevel}/${classRoom.section}"
            canvas.drawText(pageStr, pageWidth - startX - paint.measureText(pageStr), 20f, paint)

            val pageStudents = sortedStudents.drop(pageIndex * cardsPerPage).take(cardsPerPage)

            pageStudents.forEachIndexed { cardIdx, student ->
                val col = cardIdx % 2
                val row = cardIdx / 2
                val cX = startX + col * (cardWidth + gapX)
                val cY = startY + row * (cardHeight + gapY)
                val cardRect = RectF(cX, cY, cX + cardWidth, cY + cardHeight)

                // 1. Kart Arka Planı (Beyaz)
                paint.style = Paint.Style.FILL
                paint.color = Color.WHITE
                canvas.drawRoundRect(cardRect, 10f, 10f, paint)

                // 2. Kesim Çizgisi (Dashed Border)
                canvas.drawRoundRect(cardRect, 10f, 10f, dashPaint)

                // Kesim Simgesi ve Yazısı
                paint.color = cutLineColor
                paint.textSize = 7f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                canvas.drawText("✂ Kesim Çizgisi", cX + 8f, cY + 8f, paint)

                // 3. Üst Logo / Başlık Rozeti (Orijinal BİRİZ Amblemi & Kurumsal Başlık)
                if (emblemBitmap != null) {
                    val emblemRect = RectF(cX + 8f, cY + 10f, cX + 32f, cY + 34f)
                    canvas.drawBitmap(emblemBitmap, null, emblemRect, paint)
                }

                paint.color = navyPrimary
                paint.textSize = 12f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                val textX = if (emblemBitmap != null) cX + 36f else cX + 8f
                canvas.drawText("BİRİZ", textX, cY + 21f, paint)

                paint.color = Color.parseColor("#0D9488")
                paint.textSize = 6f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText("AKILLI EĞİTİM", textX, cY + 30f, paint)

                // Sağ Taraf: Veli Katılım Kartı Rozeti
                val badgeWidth = 96f
                val badgeRect = RectF(cX + cardWidth - badgeWidth - 8f, cY + 11f, cX + cardWidth - 8f, cY + 28f)
                paint.style = Paint.Style.FILL
                paint.color = Color.parseColor("#EFF6FF")
                canvas.drawRoundRect(badgeRect, 6f, 6f, paint)

                paint.style = Paint.Style.STROKE
                paint.color = Color.parseColor("#BFDBFE")
                paint.strokeWidth = 0.8f
                canvas.drawRoundRect(badgeRect, 6f, 6f, paint)
                paint.style = Paint.Style.FILL

                paint.color = navyPrimary
                paint.textSize = 7.5f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText("VELİ KATILIM KARTI", cX + cardWidth - badgeWidth + 4f, cY + 22f, paint)

                // İnce ayırıcı çizgi
                paint.color = Color.parseColor("#E2E8F0")
                paint.strokeWidth = 0.8f
                paint.style = Paint.Style.STROKE
                canvas.drawLine(cX + 8f, cY + 36f, cX + cardWidth - 8f, cY + 36f, paint)
                paint.style = Paint.Style.FILL

                // 4. Okul ve Sınıf Bilgisi
                paint.color = mutedText
                paint.textSize = 7.5f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                val schoolText = if (classRoom.schoolName.length > 30) classRoom.schoolName.take(28) + ".." else classRoom.schoolName
                canvas.drawText(schoolText, cX + 8f, cY + 43f, paint)

                paint.color = darkText
                paint.textSize = 8f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText("Sınıf: ${classRoom.gradeLevel}/${classRoom.section} • Öğrt: ${classRoom.teacherName}", cX + 8f, cY + 54f, paint)

                // 5. Öğrenci Bilgi Kutusu (Açık Mavi / Slate Kutu)
                val studentBoxRect = RectF(cX + 8f, cY + 60f, cX + cardWidth - 8f, cY + 98f)
                paint.color = studentBoxBg
                canvas.drawRoundRect(studentBoxRect, 6f, 6f, paint)

                paint.style = Paint.Style.STROKE
                paint.color = studentBoxBorder
                paint.strokeWidth = 0.8f
                canvas.drawRoundRect(studentBoxRect, 6f, 6f, paint)
                paint.style = Paint.Style.FILL

                // Öğrenci Adı
                paint.color = navyPrimary
                paint.textSize = 10f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                val displayFullName = if (student.fullName.length > 25) student.fullName.take(23) + ".." else student.fullName
                canvas.drawText("Öğrenci: $displayFullName", cX + 14f, cY + 76f, paint)

                // Okul No & Cinsiyet
                paint.color = if (student.gender.equals("Kız", ignoreCase = true)) Color.parseColor("#BE185D") else Color.parseColor("#0284C7")
                paint.textSize = 8.5f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText("Okul No: ${student.studentNumber}   •   Cinsiyet: ${student.gender}", cX + 14f, cY + 91f, paint)

                // 6. Veli Katılım Kodu Rozeti (Sol Taraf)
                val qrBoxWidth = 56f
                val qrBoxMargin = 6f
                val codeBoxWidth = cardWidth - 16f - qrBoxWidth - qrBoxMargin
                val codeBoxRect = RectF(cX + 8f, cY + 102f, cX + 8f + codeBoxWidth, cY + 152f)

                paint.color = amberBg
                canvas.drawRoundRect(codeBoxRect, 8f, 8f, paint)

                paint.style = Paint.Style.STROKE
                paint.color = amberBorder
                paint.strokeWidth = 1.2f
                canvas.drawRoundRect(codeBoxRect, 8f, 8f, paint)
                paint.style = Paint.Style.FILL

                paint.color = Color.parseColor("#B45309")
                paint.textSize = 6.8f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText("VELİ KATILIM KODU (8 HANELİ)", cX + 13f, cY + 115f, paint)

                // 8 Haneli Kod (Aralıklı ve Büyük)
                val spacedCode = student.parentInviteCode.map { "$it " }.joinToString("").trim()
                paint.color = amberText
                paint.textSize = 13.5f
                paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
                canvas.drawText(spacedCode, cX + 13f, cY + 135f, paint)

                paint.color = Color.parseColor("#B45309")
                paint.textSize = 5.2f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                canvas.drawText("Girişte bu kodu yazarak bağlanınız", cX + 13f, cY + 146f, paint)

                // 7. QR Kod Kutusu (Sağ Taraf - BİRİZ APK İndir)
                val qrBoxLeft = cX + 8f + codeBoxWidth + qrBoxMargin
                val qrBoxRect = RectF(qrBoxLeft, cY + 102f, cX + cardWidth - 8f, cY + 152f)

                paint.color = Color.parseColor("#F8FAFC")
                canvas.drawRoundRect(qrBoxRect, 8f, 8f, paint)

                paint.style = Paint.Style.STROKE
                paint.color = Color.parseColor("#CBD5E1")
                paint.strokeWidth = 0.8f
                canvas.drawRoundRect(qrBoxRect, 8f, 8f, paint)
                paint.style = Paint.Style.FILL

                if (qrBitmap != null) {
                    val qrSize = 36f
                    val qrLeft = qrBoxLeft + (qrBoxWidth - qrSize) / 2f
                    val qrTop = cY + 104f
                    val qrRect = RectF(qrLeft, qrTop, qrLeft + qrSize, qrTop + qrSize)
                    canvas.drawBitmap(qrBitmap, null, qrRect, paint)
                }

                paint.color = navyPrimary
                paint.textSize = 4.8f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                val qrTitle = "📲 APK İNDİR"
                val qrTitleW = paint.measureText(qrTitle)
                canvas.drawText(qrTitle, qrBoxLeft + (qrBoxWidth - qrTitleW) / 2f, cY + 148f, paint)

                // 8. Veli Yönergesi
                paint.color = mutedText
                paint.textSize = 6.0f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                canvas.drawText("Sayın Velimiz, yandaki karekodu telefonunuzla okutarak BİRİZ uygulamasını indirebilir,", cX + 8f, cY + 162f, paint)
                canvas.drawText("öğrenciye özel katılım kodunu girerek sınıfınıza anında bağlanabilirsiniz.", cX + 8f, cY + 171f, paint)

                paint.color = Color.parseColor("#94A3B8")
                paint.textSize = 5.6f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
                canvas.drawText("✂ Bu kartı keserek öğrencinin velisine teslim ediniz.", cX + 8f, cY + 181f, paint)
            }

            pdfDocument.finishPage(page)
        }

        val safeSchool = classRoom.schoolName.replace(" ", "_").filter { it.isLetterOrDigit() || it == '_' }
        val fileName = "${safeSchool}_${classRoom.gradeLevel}${classRoom.section}_Veli_Katilim_Kartlari.pdf"
        val file = File(context.cacheDir, fileName)

        FileOutputStream(file).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    /**
     * Sınıf panosuna asılmak ve öğrencilere dağıtılmak üzere
     * A4 Yatay (842 x 595 pt) boyutunda profesyonel BİRİZ Haftalık Ders Programı PDF belgesi üretir.
     */
    fun exportWeeklySchedulePdf(
        context: Context,
        classRoom: ClassRoom,
        config: ScheduleConfig,
        lessons: List<ScheduleLesson>
    ): Uri {
        val pdfDocument = PdfDocument()

        val pageWidth = 842 // A4 Landscape
        val pageHeight = 595
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val navyPrimary = Color.parseColor("#13294B")
        val tableHeaderBg = Color.parseColor("#1E3A8A")
        val turquoise = Color.parseColor("#28B8C2")
        val borderColor = Color.parseColor("#CBD5E1")
        val mutedText = Color.parseColor("#475569")

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // 1. Üst Kurumsal Başlık Şeridi (Mürekkep Dostu / Açık Zeminli Tasarım)
        paint.color = Color.WHITE
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), 72f, paint)

        // Altına kurumsal ayırıcı çizgi ve turkuaz vurgu
        paint.color = borderColor
        paint.strokeWidth = 0.8f
        canvas.drawLine(0f, 70f, pageWidth.toFloat(), 70f, paint)
        paint.color = turquoise
        paint.strokeWidth = 2.5f
        canvas.drawLine(0f, 71.5f, pageWidth.toFloat(), 71.5f, paint)
        paint.strokeWidth = 1f

        // BİRİZ Amblemi
        val emblemBitmap = renderEmblemBitmap(context, 140)
        var textStartX = 32f
        if (emblemBitmap != null) {
            val emblemRect = RectF(32f, 12f, 32f + 48f, 12f + 48f)
            canvas.drawBitmap(emblemBitmap, null, emblemRect, paint)
            textStartX = 88f
        }

        // BİRİZ Kurumsal Başlık Metinleri (3 Ayrı ve Ferah Satır, Asla Üst Üste Binmez)
        // 1. Satır: Ana Marka
        paint.color = navyPrimary
        paint.textSize = 18f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("BİRİZ", textStartX, 28f, paint)

        // 2. Satır: Platform Tanımı
        paint.color = Color.parseColor("#0D9488")
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("AKILLI EĞİTİM PLATFORMU", textStartX, 42f, paint)

        // 3. Satır: Program Başlığı
        paint.color = Color.parseColor("#B45309")
        paint.textSize = 11.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("HAFTALIK DERS PROGRAMI", textStartX, 57f, paint)

        // Sağ Üst: Okul, Sınıf ve Öğretmen Bilgisi (Açık zeminde net okunan koyu renkler)
        paint.color = navyPrimary
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val schoolText = "${classRoom.schoolName} • ${classRoom.gradeLevel}/${classRoom.section} Şubesi"
        val schoolTextWidth = paint.measureText(schoolText)
        canvas.drawText(schoolText, (pageWidth - 32f - schoolTextWidth), 34f, paint)

        paint.color = mutedText
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        val teacherDateText = "Sınıf Öğretmeni: ${classRoom.teacherName} • Tarih: ${SimpleDateFormat("dd.MM.yyyy", Locale("tr", "TR")).format(Date())}"
        val teacherDateWidth = paint.measureText(teacherDateText)
        canvas.drawText(teacherDateText, (pageWidth - 32f - teacherDateWidth), 52f, paint)

        // 2. Tablo Düzeni
        val tableLeft = 32f
        val tableRight = (pageWidth - 32).toFloat()
        val totalTableWidth = tableRight - tableLeft // 778f
        val timeColWidth = 118f
        val dayColWidth = (totalTableWidth - timeColWidth) / 5f // 132f each

        var currentY = 86f
        val headerRowHeight = 28f

        // Tablo Başlık Satırı (Mürekkep Tasarruflu Açık Zemin)
        val headerRect = RectF(tableLeft, currentY, tableRight, currentY + headerRowHeight)
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#F1F5F9")
        canvas.drawRoundRect(headerRect, 6f, 6f, paint)

        paint.style = Paint.Style.STROKE
        paint.color = borderColor
        paint.strokeWidth = 1f
        canvas.drawRoundRect(headerRect, 6f, 6f, paint)
        paint.style = Paint.Style.FILL

        paint.color = navyPrimary
        paint.textSize = 11f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        canvas.drawText("Ders / Saat", tableLeft + 14f, currentY + 18f, paint)

        val days = ScheduleHelper.DAYS_OF_WEEK
        days.forEachIndexed { idx, dayKey ->
            val colX = tableLeft + timeColWidth + idx * dayColWidth
            val dayName = ScheduleHelper.DAY_NAMES[dayKey] ?: dayKey
            val dayNameWidth = paint.measureText(dayName)
            val centerX = colX + (dayColWidth - dayNameWidth) / 2f
            canvas.drawText(dayName, centerX, currentY + 18f, paint)
        }

        currentY += headerRowHeight + 4f

        // Ders Saatlerini Hesapla
        var runningTime = config.firstLessonStartTime
        var recessIndex = 0
        val lessonCount = config.dailyLessonCount
        val totalAvailableHeight = pageHeight - currentY - 40f
        val hasLunch = config.isLunchBreakActive && config.lunchAfterLessonIndex in 1 until lessonCount
        val rowSlotsCount = lessonCount + (if (hasLunch) 1 else 0)
        val lessonRowHeight = minOf(64f, (totalAvailableHeight / rowSlotsCount))

        for (i in 1..lessonCount) {
            val lessonEndTime = ScheduleHelper.addMinutes(runningTime, config.lessonDurationMinutes)

            // Satır Arka Planı
            val rowRect = RectF(tableLeft, currentY, tableRight, currentY + lessonRowHeight - 3f)
            paint.color = if (i % 2 == 0) Color.parseColor("#F8FAFC") else Color.WHITE
            canvas.drawRoundRect(rowRect, 6f, 6f, paint)

            paint.style = Paint.Style.STROKE
            paint.color = borderColor
            paint.strokeWidth = 0.6f
            canvas.drawRoundRect(rowRect, 6f, 6f, paint)
            paint.style = Paint.Style.FILL

            // 1. Sütun: Ders No ve Saat
            paint.color = navyPrimary
            paint.textSize = 11f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("${i}. Ders", tableLeft + 14f, currentY + (lessonRowHeight / 2f) - 2f, paint)

            paint.color = mutedText
            paint.textSize = 9f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText("$runningTime - $lessonEndTime", tableLeft + 14f, currentY + (lessonRowHeight / 2f) + 12f, paint)

            // Dikey ayırıcı çizgi
            paint.color = borderColor
            paint.strokeWidth = 0.6f
            canvas.drawLine(tableLeft + timeColWidth, currentY, tableLeft + timeColWidth, currentY + lessonRowHeight - 3f, paint)

            // 5 Günün Dersleri
            days.forEachIndexed { dIdx, dayKey ->
                val colStartX = tableLeft + timeColWidth + dIdx * dayColWidth
                val colEndX = colStartX + dayColWidth

                // Sütun ayracı
                if (dIdx > 0) {
                    paint.color = borderColor
                    paint.strokeWidth = 0.5f
                    canvas.drawLine(colStartX, currentY, colStartX, currentY + lessonRowHeight - 3f, paint)
                }

                val matchedLesson = lessons.firstOrNull { it.dayOfWeek == dayKey && it.lessonIndex == i }
                if (matchedLesson != null && matchedLesson.subjectName.isNotBlank()) {
                    val colors = ScheduleHelper.getColorsForTheme(matchedLesson.colorTheme)
                    val cardBadgeRect = RectF(colStartX + 5f, currentY + 4f, colEndX - 5f, currentY + lessonRowHeight - 7f)

                    // Kart rengi
                    paint.color = Color.parseColor(colors.first)
                    canvas.drawRoundRect(cardBadgeRect, 6f, 6f, paint)

                    // Ders Adı
                    paint.color = Color.WHITE
                    paint.textSize = 10f
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    val subject = TurkishTextHelper.toTurkishUpperCase(matchedLesson.subjectName)
                    val textW = paint.measureText(subject)
                    val badgeCenterX = cardBadgeRect.left + (cardBadgeRect.width() - textW) / 2f
                    val badgeCenterY = cardBadgeRect.centerY() + 4f
                    canvas.drawText(subject, maxOf(cardBadgeRect.left + 4f, badgeCenterX), badgeCenterY, paint)
                } else {
                    paint.color = Color.parseColor("#94A3B8")
                    paint.textSize = 11f
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                    canvas.drawText("—", colStartX + (dayColWidth / 2f) - 4f, currentY + (lessonRowHeight / 2f) + 4f, paint)
                }
            }

            currentY += lessonRowHeight
            runningTime = lessonEndTime

            // Öğle Arası Şeridi (Belirlenen dersten sonra)
            if (config.isLunchBreakActive && i == config.lunchAfterLessonIndex) {
                val lunchEnd = ScheduleHelper.addMinutes(runningTime, config.lunchDurationMinutes)
                val lunchHeight = 22f
                val lunchRect = RectF(tableLeft, currentY, tableRight, currentY + lunchHeight)

                paint.color = Color.parseColor("#FEF3C7")
                canvas.drawRoundRect(lunchRect, 6f, 6f, paint)

                paint.style = Paint.Style.STROKE
                paint.color = Color.parseColor("#F59E0B")
                paint.strokeWidth = 0.8f
                canvas.drawRoundRect(lunchRect, 6f, 6f, paint)
                paint.style = Paint.Style.FILL

                paint.color = Color.parseColor("#B45309")
                paint.textSize = 9.5f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                val lunchText = "🍽️  ÖĞLE ARASI   ($runningTime - $lunchEnd • ${config.lunchDurationMinutes} Dakika)"
                val lw = paint.measureText(lunchText)
                canvas.drawText(lunchText, tableLeft + (totalTableWidth - lw) / 2f, currentY + 15f, paint)

                currentY += lunchHeight + 4f
                runningTime = lunchEnd
            } else if (i < lessonCount) {
                val recessDur = config.recessDurations.getOrElse(recessIndex) { 15 }
                runningTime = ScheduleHelper.addMinutes(runningTime, recessDur)
                recessIndex++
            }
        }

        // 3. Alt Dipnot Bilgisi
        paint.color = mutedText
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText(
            "Bu haftalık ders programı BİRİZ Akıllı Sınıf ve Eğitim Platformu tarafından sınıf panosu ve öğrenci kullanımı için hazırlanmıştır.",
            tableLeft,
            (pageHeight - 16).toFloat(),
            paint
        )

        pdfDocument.finishPage(page)

        val safeSchool = classRoom.schoolName.replace(" ", "_").filter { it.isLetterOrDigit() || it == '_' }
        val fileName = "${safeSchool}_${classRoom.gradeLevel}${classRoom.section}_Haftalik_Ders_Programi.pdf"
        val file = File(context.cacheDir, fileName)

        FileOutputStream(file).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    /**
     * Akıllı Sınıf Oturma Planını BİRİZ Kurumsal Kimliğine uygun
     * A4 Yatay (Landscape) mürekkep dostu PDF belgesi olarak üretir.
     */
    fun exportSeatingPlanPdf(
        context: Context,
        classRoom: ClassRoom,
        seatingPlanState: SeatingPlanState,
        students: List<Student>
    ): Uri {
        val pdfDocument = PdfDocument()

        // A4 Yatay (Landscape) boyutları: 842 x 595 nokta
        val pageWidth = 842
        val pageHeight = 595
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val navyPrimary = Color.parseColor("#13294B")
        val turquoise = Color.parseColor("#28B8C2")
        val tealColor = Color.parseColor("#0D9488")
        val amber = Color.parseColor("#D97706")
        val borderColor = Color.parseColor("#CBD5E1")
        val darkText = Color.parseColor("#0F172A")
        val mutedText = Color.parseColor("#475569")
        val boardBg = Color.parseColor("#334155")
        val deskSurfaceBg = Color.parseColor("#F8FAFC")

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // 1. Zemin (Açık Beyaz, Mürekkep Dostu)
        canvas.drawColor(Color.WHITE)

        val emblemBitmap = renderEmblemBitmap(context, 140)

        // Üst Başlık Şeridi
        paint.color = Color.WHITE
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), 76f, paint)

        // Alt sınır ve turkuaz kurumsal vurgu çizgisi
        paint.color = borderColor
        paint.strokeWidth = 1f
        canvas.drawLine(0f, 74.5f, pageWidth.toFloat(), 74.5f, paint)
        paint.color = turquoise
        paint.strokeWidth = 2.5f
        canvas.drawLine(0f, 76f, pageWidth.toFloat(), 76f, paint)
        paint.strokeWidth = 1f

        // BİRİZ Logo Amblemi
        var textStartX = 36f
        if (emblemBitmap != null) {
            val emblemRect = RectF(36f, 12f, 36f + 52f, 12f + 52f)
            canvas.drawBitmap(emblemBitmap, null, emblemRect, paint)
            textStartX = 98f
        }

        // BİRİZ Kurumsal Başlık Metinleri (3 Ayrı Satır, Sıfır Çakışma)
        paint.color = navyPrimary
        paint.textSize = 19f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("BİRİZ", textStartX, 31f, paint)

        paint.color = tealColor
        paint.textSize = 9.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("AKILLI SINIF VE EĞİTİM PLATFORMU", textStartX, 46f, paint)

        paint.color = mutedText
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Sınıf Yönetimi • Akıllı Oturma Düzeni ve Yerleşim Krokisi", textStartX, 60f, paint)

        // Sağ Üst Bilgi Kartı
        val infoBoxRight = pageWidth - 36f
        paint.color = darkText
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val schoolText = "${classRoom.schoolName} • ${classRoom.gradeLevel}/${classRoom.section} Şubesi"
        val schoolWidth = paint.measureText(schoolText)
        canvas.drawText(schoolText, infoBoxRight - schoolWidth, 28f, paint)

        paint.color = mutedText
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        val teacherText = "Sınıf Öğretmeni: ${classRoom.teacherName}"
        val teacherWidth = paint.measureText(teacherText)
        canvas.drawText(teacherText, infoBoxRight - teacherWidth, 44f, paint)

        val dateStr = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("tr", "TR")).format(Date())
        val seatedCount = seatingPlanState.assignments.values.filter { it.isNotBlank() }.distinct().size
        val statsText = "$dateStr • $seatedCount / ${students.size} Öğrenci Yerleşik (${seatingPlanState.config.totalSeats} Koltuk)"
        val statsWidth = paint.measureText(statsText)
        canvas.drawText(statsText, infoBoxRight - statsWidth, 59f, paint)

        // 2. ÖN CEPHE MİMARİ ŞERİDİ (Ortalanmış Akıllı Tahta, Öğretmen Masası, Kapı ve Pencereler)
        val krokiY = 84f
        val krokiHeight = 26f
        val krokiLeft = 36f
        val krokiRight = pageWidth - 36f

        // Akıllı Tahta (TAM ORTAYA HİZALANMIŞ)
        val boardWidth = 220f
        val boardLeft = (pageWidth - boardWidth) / 2f
        val boardRect = RectF(boardLeft, krokiY, boardLeft + boardWidth, krokiY + krokiHeight)
        paint.color = boardBg
        canvas.drawRoundRect(boardRect, 6f, 6f, paint)
        paint.color = Color.WHITE
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val boardTitle = "📋 AKILLI TAHTA / YAZI ALANI"
        val btw = paint.measureText(boardTitle)
        canvas.drawText(boardTitle, boardLeft + (boardWidth - btw) / 2f, krokiY + 17f, paint)

        // Öğretmen Masası Konumu (Sol Ön, Orta Ön veya Sağ Ön)
        val deskWidth = 110f
        val deskLeft = when (seatingPlanState.config.deskPosition) {
            DeskPosition.LEFT -> boardLeft - deskWidth - 12f
            DeskPosition.RIGHT -> boardLeft + boardWidth + 12f
            DeskPosition.CENTER -> boardLeft + (boardWidth - deskWidth) / 2f
        }
        val teacherDeskRect = RectF(deskLeft, krokiY, deskLeft + deskWidth, krokiY + krokiHeight)
        paint.color = Color.parseColor("#FEF3C7")
        canvas.drawRoundRect(teacherDeskRect, 6f, 6f, paint)
        paint.style = Paint.Style.STROKE
        paint.color = amber
        paint.strokeWidth = 1f
        canvas.drawRoundRect(teacherDeskRect, 6f, 6f, paint)
        paint.style = Paint.Style.FILL

        paint.color = Color.parseColor("#B45309")
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("👩‍🏫 Öğretmen Masası", deskLeft + 8f, krokiY + 17f, paint)

        // Ön Cephe veya Yan Ön Kapı Göstergesi
        val dp = seatingPlanState.config.doorPosition
        val hasFrontLeftDoor = dp == DoorPosition.ON_SOL || dp == DoorPosition.SOL_ON
        val hasFrontRightDoor = dp == DoorPosition.ON_SAG || dp == DoorPosition.SAG_ON

        if (hasFrontLeftDoor) {
            val doorRect = RectF(krokiLeft, krokiY, krokiLeft + 75f, krokiY + krokiHeight)
            paint.color = Color.parseColor("#FEF3C7")
            canvas.drawRoundRect(doorRect, 6f, 6f, paint)
            paint.color = Color.parseColor("#B45309")
            paint.textSize = 9f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("🚪 Kapı (${dp.positionOnFacade})", krokiLeft + 8f, krokiY + 17f, paint)
        }

        if (hasFrontRightDoor) {
            val doorRect = RectF(krokiRight - 75f, krokiY, krokiRight, krokiY + krokiHeight)
            paint.color = Color.parseColor("#FEF3C7")
            canvas.drawRoundRect(doorRect, 6f, 6f, paint)
            paint.color = Color.parseColor("#B45309")
            paint.textSize = 9f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("🚪 Kapı (${dp.positionOnFacade})", krokiRight - 70f, krokiY + 17f, paint)
        }

        // Pencere Göstergesi (Sol veya Sağ Cephe)
        val wp = seatingPlanState.config.windowPosition
        if (wp == WindowPosition.LEFT) {
            val winLeft = if (hasFrontLeftDoor) krokiLeft + 80f else krokiLeft
            val winRect = RectF(winLeft, krokiY, winLeft + 85f, krokiY + krokiHeight)
            paint.color = Color.parseColor("#E0F2FE")
            canvas.drawRoundRect(winRect, 6f, 6f, paint)
            paint.color = Color.parseColor("#0284C7")
            paint.textSize = 9f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("🪟 Pencere", winLeft + 14f, krokiY + 17f, paint)
        } else if (wp == WindowPosition.RIGHT) {
            val winRight = if (hasFrontRightDoor) krokiRight - 80f else krokiRight
            val winRect = RectF(winRight - 85f, krokiY, winRight, krokiY + krokiHeight)
            paint.color = Color.parseColor("#E0F2FE")
            canvas.drawRoundRect(winRect, 6f, 6f, paint)
            paint.color = Color.parseColor("#0284C7")
            paint.textSize = 9f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("🪟 Pencere", winRight - 75f, krokiY + 17f, paint)
        }

        // 3. GRUP VE SIRA YERLEŞİM ALANI
        val config = seatingPlanState.config
        val groupCount = config.groupCount.coerceIn(1, 6)
        val peoplePerRow = config.peoplePerRow.coerceIn(1, 4)
        val maxRows = (config.rowsPerGroup.maxOrNull() ?: 5).coerceIn(1, 8)

        val layoutTop = 118f
        val layoutBottom = 530f
        val totalLayoutHeight = layoutBottom - layoutTop

        val groupGap = 16f
        val totalGroupWidth = krokiRight - krokiLeft
        val groupWidth = (totalGroupWidth - (groupCount - 1) * groupGap) / groupCount

        // Her sıra için dikey yükseklik
        val rowGap = 7f
        val deskHeight = ((totalLayoutHeight - 20f - (maxRows * rowGap)) / maxRows).coerceIn(40f, 66f)

        for (g in 0 until groupCount) {
            val groupStartX = krokiLeft + g * (groupWidth + groupGap)
            val rowsInThisGroup = config.rowsPerGroup.getOrElse(g) { 5 }

            // Grup Başlığı
            val groupHeaderRect = RectF(groupStartX, layoutTop, groupStartX + groupWidth, layoutTop + 18f)
            paint.color = Color.parseColor("#F1F5F9")
            canvas.drawRoundRect(groupHeaderRect, 4f, 4f, paint)

            paint.color = navyPrimary
            paint.textSize = 9f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val gTitle = "${g + 1}. Grup • $rowsInThisGroup Sıra (${rowsInThisGroup * peoplePerRow} Koltuk)"
            val gtw = paint.measureText(gTitle)
            canvas.drawText(gTitle, groupStartX + (groupWidth - gtw) / 2f, layoutTop + 13f, paint)

            var currentDeskY = layoutTop + 24f

            for (r in 0 until rowsInThisGroup) {
                val deskRect = RectF(groupStartX, currentDeskY, groupStartX + groupWidth, currentDeskY + deskHeight)

                // Sıra Gövdesi
                paint.color = deskSurfaceBg
                canvas.drawRoundRect(deskRect, 6f, 6f, paint)

                paint.style = Paint.Style.STROKE
                paint.color = borderColor
                paint.strokeWidth = 0.8f
                canvas.drawRoundRect(deskRect, 6f, 6f, paint)
                paint.style = Paint.Style.FILL

                // Sıra Numarası Etiketi (Sol Üst Köşe)
                paint.color = mutedText
                paint.textSize = 7.5f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                canvas.drawText("${r + 1}. Sıra", groupStartX + 6f, currentDeskY + 10f, paint)

                // Koltukları Böl
                val seatPad = 4f
                val availableSeatWidth = groupWidth - (seatPad * 2f) - ((peoplePerRow - 1) * 3f)
                val seatWidth = availableSeatWidth / peoplePerRow
                val seatTop = currentDeskY + 13f
                val seatHeight = deskHeight - 16f

                for (s in 0 until peoplePerRow) {
                    val seatLeft = groupStartX + seatPad + s * (seatWidth + 3f)
                    val seatRect = RectF(seatLeft, seatTop, seatLeft + seatWidth, seatTop + seatHeight)

                    val seatId = "g$g-r$r-s$s"
                    val studentId = seatingPlanState.assignments[seatId]
                    val student = students.firstOrNull { it.id == studentId }

                    if (student != null) {
                        val isGirl = student.gender.equals("Kız", ignoreCase = true)
                        val isPinned = seatingPlanState.pinnedStudents.contains(student.id)
                        val isPriority = seatingPlanState.priorityStudents.contains(student.id)

                        // Zemin
                        paint.color = if (isGirl) Color.parseColor("#FCE7F3") else Color.parseColor("#E0F2FE")
                        canvas.drawRoundRect(seatRect, 4f, 4f, paint)

                        // Kenarlık
                        paint.style = Paint.Style.STROKE
                        paint.color = if (isGirl) Color.parseColor("#F472B6") else Color.parseColor("#93C5FD")
                        paint.strokeWidth = 1f
                        canvas.drawRoundRect(seatRect, 4f, 4f, paint)
                        paint.style = Paint.Style.FILL

                        // Öğrenci Numarası ve İkonlar
                        val studentTextColor = if (isGirl) Color.parseColor("#9D174D") else Color.parseColor("#1E40AF")
                        paint.color = studentTextColor
                        paint.textSize = 8.5f
                        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

                        val badgeIcons = buildString {
                            if (isPinned) append("🔒")
                            if (isPriority) append("👓")
                        }
                        val noText = "${student.studentNumber} $badgeIcons".trim()
                        canvas.drawText(noText, seatLeft + 4f, seatTop + 11f, paint)

                        // Öğrenci Adı Soyadı (Büyük Harflerle, Türkçe Kurallarına Uygun)
                        paint.textSize = 8.5f
                        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        val fullNameUpper = TurkishTextHelper.toTurkishUpperCase(student.fullName)
                        val maxNameWidth = seatWidth - 8f
                        var displayName = fullNameUpper
                        if (paint.measureText(displayName) > maxNameWidth) {
                            while (displayName.isNotEmpty() && paint.measureText("$displayName…") > maxNameWidth) {
                                displayName = displayName.dropLast(1)
                            }
                            displayName = "$displayName…"
                        }
                        canvas.drawText(displayName, seatLeft + 4f, seatTop + seatHeight - 5f, paint)
                    } else {
                        // Boş Koltuk
                        paint.color = Color.WHITE
                        canvas.drawRoundRect(seatRect, 4f, 4f, paint)

                        paint.style = Paint.Style.STROKE
                        paint.color = Color.parseColor("#E2E8F0")
                        paint.strokeWidth = 0.8f
                        canvas.drawRoundRect(seatRect, 4f, 4f, paint)
                        paint.style = Paint.Style.FILL

                        paint.color = Color.parseColor("#94A3B8")
                        paint.textSize = 8f
                        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                        val emptyLabel = "+ Boş"
                        val elw = paint.measureText(emptyLabel)
                        canvas.drawText(emptyLabel, seatLeft + (seatWidth - elw) / 2f, seatTop + (seatHeight / 2f) + 3f, paint)
                    }
                }

                currentDeskY += deskHeight + rowGap
            }
        }

        // 4. ARKA CEPHE GÖSTERGESİ (Arka Pencereler, Arka Kapı ve Panolar)
        val hasBackLeftDoor = dp == DoorPosition.ARKA_SOL || dp == DoorPosition.SOL_ARKA
        val hasBackRightDoor = dp == DoorPosition.ARKA_SAG || dp == DoorPosition.SAG_ARKA
        val hasBackWindow = wp == WindowPosition.BACK

        val backWallY = 533f
        val backWallHeight = 16f
        val backWallRect = RectF(krokiLeft, backWallY, krokiRight, backWallY + backWallHeight)

        paint.color = Color.parseColor("#F8FAFC")
        canvas.drawRoundRect(backWallRect, 4f, 4f, paint)

        paint.style = Paint.Style.STROKE
        paint.color = Color.parseColor("#E2E8F0")
        paint.strokeWidth = 0.8f
        canvas.drawRoundRect(backWallRect, 4f, 4f, paint)
        paint.style = Paint.Style.FILL

        // Arka Cephe Başlığı (Ortalanmış)
        paint.color = Color.parseColor("#64748B")
        paint.textSize = 7.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val backTitle = if (hasBackWindow) "🪟 Arka Cephe Pencereleri & Sınıf Panosu" else "— ARKA CEPHE (SINIF ARKASI & PANOLAR) —"
        val backTextWidth = paint.measureText(backTitle)
        canvas.drawText(backTitle, (pageWidth - backTextWidth) / 2f, backWallY + 11f, paint)

        // Sol Arka Kapı
        if (hasBackLeftDoor) {
            val bDoorRect = RectF(krokiLeft + 2f, backWallY + 2f, krokiLeft + 74f, backWallY + backWallHeight - 2f)
            paint.color = Color.parseColor("#FEF3C7")
            canvas.drawRoundRect(bDoorRect, 3f, 3f, paint)
            paint.color = Color.parseColor("#B45309")
            paint.textSize = 7.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("🚪 Kapı (${dp.positionOnFacade})", krokiLeft + 6f, backWallY + 10.5f, paint)
        }

        // Sağ Arka Kapı
        if (hasBackRightDoor) {
            val bDoorRect = RectF(krokiRight - 74f, backWallY + 2f, krokiRight - 2f, backWallY + backWallHeight - 2f)
            paint.color = Color.parseColor("#FEF3C7")
            canvas.drawRoundRect(bDoorRect, 3f, 3f, paint)
            paint.color = Color.parseColor("#B45309")
            paint.textSize = 7.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("🚪 Kapı (${dp.positionOnFacade})", krokiRight - 70f, backWallY + 10.5f, paint)
        }

        // 5. ALT BİLGİ & AÇIKLAMA ŞERİDİ (LEJANT & İSTATİSTİK)
        val footerY = 556f

        // Lejant Kutucukları
        // Kız Lejant
        val fLegendRect = RectF(krokiLeft, footerY, krokiLeft + 14f, footerY + 12f)
        paint.color = Color.parseColor("#FCE7F3")
        canvas.drawRoundRect(fLegendRect, 2f, 2f, paint)
        paint.style = Paint.Style.STROKE
        paint.color = Color.parseColor("#F472B6")
        canvas.drawRoundRect(fLegendRect, 2f, 2f, paint)
        paint.style = Paint.Style.FILL

        paint.color = darkText
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Kız Öğrenci", krokiLeft + 18f, footerY + 9.5f, paint)

        // Erkek Lejant
        val mLegendRect = RectF(krokiLeft + 75f, footerY, krokiLeft + 89f, footerY + 12f)
        paint.color = Color.parseColor("#E0F2FE")
        canvas.drawRoundRect(mLegendRect, 2f, 2f, paint)
        paint.style = Paint.Style.STROKE
        paint.color = Color.parseColor("#93C5FD")
        canvas.drawRoundRect(mLegendRect, 2f, 2f, paint)
        paint.style = Paint.Style.FILL

        paint.color = darkText
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Erkek Öğrenci", krokiLeft + 93f, footerY + 9.5f, paint)

        // Öncelik ve Kilit Lejantları
        canvas.drawText("👓 Ön Sıra Önceliği", krokiLeft + 160f, footerY + 9.5f, paint)
        canvas.drawText("🔒 Sabitlenmiş Sıra", krokiLeft + 250f, footerY + 9.5f, paint)

        // Sağ Alt Dipnot
        paint.color = mutedText
        paint.textSize = 8.5f
        val noteText = "Bu plan BİRİZ Akıllı Sınıf Platformu tarafından sınıf içi pedagojik denge ve ergonomi gözetilerek üretilmiştir."
        val ntw = paint.measureText(noteText)
        canvas.drawText(noteText, krokiRight - ntw, footerY + 9.5f, paint)

        pdfDocument.finishPage(page)

        val safeSchool = classRoom.schoolName.replace(" ", "_").filter { it.isLetterOrDigit() || it == '_' }
        val fileName = "${safeSchool}_${classRoom.gradeLevel}${classRoom.section}_Sinif_Oturma_Plani.pdf"
        val file = File(context.cacheDir, fileName)

        FileOutputStream(file).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    /**
     * Sınıf sıralarının köşelerine yapıştırılmak üzere kesilebilir mini masa isimlikleri (A4 PDF) üretir.
     * Her A4 sayfada 10 adet (2 sütun x 5 satır) kesim çizgili, öğrenci no, ad-soyad, grup ve sıra bilgili,
     * BİRİZ logolu şık masa etiketleri yer alır.
     */
    fun exportDeskNameTagsPdf(
        context: Context,
        classRoom: ClassRoom,
        seatingPlanState: SeatingPlanState,
        students: List<Student>
    ): Uri {
        val pdfDocument = PdfDocument()

        val pageWidth = 595
        val pageHeight = 842

        val tagsPerPage = 10
        val sortedStudents = students.sortedBy { it.studentNumber.toIntOrNull() ?: 9999 }
        val totalPages = maxOf(1, (sortedStudents.size + tagsPerPage - 1) / tagsPerPage)

        val cardWidth = 262f
        val cardHeight = 140f
        val startX = 28f
        val startY = 34f
        val gapX = 15f
        val gapY = 16f

        val navyPrimary = Color.parseColor("#13294B")
        val darkText = Color.parseColor("#0F172A")
        val mutedText = Color.parseColor("#64748B")
        val cutLineColor = Color.parseColor("#94A3B8")

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val emblemBitmap = renderEmblemBitmap(context, 100)
        val dashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1f
            color = cutLineColor
            pathEffect = DashPathEffect(floatArrayOf(5f, 4f), 0f)
        }

        val studentSeatMap = mutableMapOf<String, String>()
        seatingPlanState.assignments.forEach { (seatId, studentId) ->
            if (studentId.isNotBlank()) {
                val coord = SeatingPlanHelper.parseSeatId(seatId)
                if (coord != null) {
                    val side = if (coord.seatIndex == 0) "Sol" else "Sağ"
                    studentSeatMap[studentId] = "${coord.groupIndex + 1}. Grup • ${coord.rowIndex + 1}. Sıra ($side)"
                }
            }
        }

        for (pageIndex in 0 until totalPages) {
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex + 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            // Sayfa Üst Bilgisi
            paint.color = navyPrimary
            paint.textSize = 8.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("BİRİZ • Sıra Üstü Kesilebilir İsimlik ve Masa Etiketleri", startX, 20f, paint)

            paint.color = mutedText
            paint.textSize = 8f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            val pageStr = "Sayfa ${pageIndex + 1} / $totalPages • ${classRoom.schoolName} - ${classRoom.gradeLevel}/${classRoom.section}"
            canvas.drawText(pageStr, pageWidth - startX - paint.measureText(pageStr), 20f, paint)

            val fromIndex = pageIndex * tagsPerPage
            val toIndex = minOf(fromIndex + tagsPerPage, sortedStudents.size)
            val pageStudents = sortedStudents.subList(fromIndex, toIndex)

            for (i in pageStudents.indices) {
                val student = pageStudents[i]
                val isKiz = student.gender.equals("Kız", ignoreCase = true)

                val col = i % 2
                val row = i / 2
                val x = startX + col * (cardWidth + gapX)
                val y = startY + row * (cardHeight + gapY)
                val cardRect = RectF(x, y, x + cardWidth, y + cardHeight)

                // Dış Kesim Çizgisi (Dashed Border)
                canvas.drawRoundRect(cardRect, 8f, 8f, dashPaint)

                // Kart Arka Planı
                paint.style = Paint.Style.FILL
                paint.color = Color.parseColor("#FAFAFA")
                canvas.drawRoundRect(cardRect, 8f, 8f, paint)

                // Makas İkonu & Kesim Uyarısı
                paint.color = cutLineColor
                paint.textSize = 7f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                canvas.drawText("✂️ Buradan Kesiniz", x + 8f, y + 10f, paint)

                // Üst Başlık Şeridi: Okul ve Sınıf
                val headerRect = RectF(x + 6f, y + 14f, x + cardWidth - 6f, y + 36f)
                paint.color = if (isKiz) Color.parseColor("#FCE7F3") else Color.parseColor("#E0F2FE")
                canvas.drawRoundRect(headerRect, 6f, 6f, paint)

                paint.color = if (isKiz) Color.parseColor("#9D174D") else Color.parseColor("#075985")
                paint.textSize = 8.5f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                val schoolHeader = "${classRoom.schoolName} • ${classRoom.gradeLevel}/${classRoom.section}"
                var displayHeader = schoolHeader
                if (paint.measureText(displayHeader) > cardWidth - 24f) {
                    while (displayHeader.isNotEmpty() && paint.measureText("$displayHeader…") > cardWidth - 24f) {
                        displayHeader = displayHeader.dropLast(1)
                    }
                    displayHeader = "$displayHeader…"
                }
                canvas.drawText(displayHeader, x + 12f, y + 28f, paint)

                // Sol: Öğrenci Okul No Rozeti
                val noBadgeRect = RectF(x + 10f, y + 44f, x + 62f, y + 96f)
                paint.color = if (isKiz) Color.parseColor("#FDF2F8") else Color.parseColor("#F0F9FF")
                canvas.drawRoundRect(noBadgeRect, 8f, 8f, paint)

                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 1.2f
                paint.color = if (isKiz) Color.parseColor("#F472B6") else Color.parseColor("#38BDF8")
                canvas.drawRoundRect(noBadgeRect, 8f, 8f, paint)
                paint.style = Paint.Style.FILL

                paint.color = if (isKiz) Color.parseColor("#BE185D") else Color.parseColor("#0284C7")
                paint.textSize = 10f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText("NO", x + 28f, y + 60f, paint)

                paint.textSize = 18f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                val noW = paint.measureText(student.studentNumber)
                canvas.drawText(student.studentNumber, x + 10f + (52f - noW) / 2f, y + 84f, paint)

                // Orta-Sağ: Öğrenci Adı Soyadı
                val nameX = x + 72f
                paint.color = darkText
                paint.textSize = 12.5f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                val fullUpper = TurkishTextHelper.toTurkishUpperCase(student.fullName)
                val maxNameW = cardWidth - 80f
                var dispName = fullUpper
                if (paint.measureText(dispName) > maxNameW) {
                    while (dispName.isNotEmpty() && paint.measureText("$dispName…") > maxNameW) {
                        dispName = dispName.dropLast(1)
                    }
                    dispName = "$dispName…"
                }
                canvas.drawText(dispName, nameX, y + 62f, paint)

                // Sıra Konumu Bilgisi
                val seatLocation = studentSeatMap[student.id] ?: "Henüz Sıra Atanmadı"
                paint.color = if (studentSeatMap.containsKey(student.id)) Color.parseColor("#0F766E") else mutedText
                paint.textSize = 8.5f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText("📍 $seatLocation", nameX, y + 78f, paint)

                // Logo veya BİRİZ Rozeti
                if (emblemBitmap != null) {
                    val bmpRect = Rect(0, 0, emblemBitmap.width, emblemBitmap.height)
                    val destRect = RectF(x + cardWidth - 32f, y + 42f, x + cardWidth - 8f, y + 66f)
                    canvas.drawBitmap(emblemBitmap, bmpRect, destRect, null)
                }

                // Alt Kısım Şeridi: Motivasyon Sözü
                val bottomBannerRect = RectF(x + 6f, y + 104f, x + cardWidth - 6f, y + 132f)
                paint.color = Color.parseColor("#F1F5F9")
                canvas.drawRoundRect(bottomBannerRect, 6f, 6f, paint)

                paint.color = navyPrimary
                paint.textSize = 8f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                val motto = "🌟 Başarılı ve Mutlu Bir Ders Yılı Dileriz! • BİRİZ"
                canvas.drawText(motto, x + 16f, y + 121f, paint)
            }

            pdfDocument.finishPage(page)
        }

        val safeSchool = classRoom.schoolName.replace(" ", "_").filter { it.isLetterOrDigit() || it == '_' }
        val fileName = "${safeSchool}_${classRoom.gradeLevel}${classRoom.section}_Sira_Ustu_Isimlikler.pdf"
        val file = File(context.cacheDir, fileName)

        FileOutputStream(file).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    /**
     * Sınıf Çalışma Grupları Listesini (.xlsx) Excel Formatında Dışa Aktarır.
     */
    fun exportGroupListExcel(
        context: Context,
        classRoom: ClassRoom,
        groups: List<GroupTeam>,
        allStudents: List<Student>
    ): Uri {
        val safeSchool = classRoom.schoolName.replace(" ", "_").filter { it.isLetterOrDigit() || it == '_' }
        val fileName = "${safeSchool}_${classRoom.gradeLevel}${classRoom.section}_Grup_Listesi.xlsx"
        val file = File(context.cacheDir, fileName)

        val headers = listOf("Grup Adı", "Sıra No", "Öğrenci No", "Adı Soyadı", "Cinsiyet", "Grup Rolü")
        val rows = mutableListOf<List<String>>()

        var overallIndex = 1
        groups.forEach { group ->
            val members = group.studentIds.mapNotNull { sid -> allStudents.firstOrNull { it.id == sid } }
            members.forEach { student ->
                val isLeader = student.id == group.leaderStudentId
                val role = if (isLeader) "Grup Sözcüsü / Lideri (👑)" else "Grup Üyesi"
                rows.add(
                    listOf(
                        group.name,
                        overallIndex.toString(),
                        student.studentNumber,
                        student.fullName,
                        student.gender,
                        role
                    )
                )
                overallIndex++
            }
        }

        writeXlsx(file, headers, rows)

        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    /**
     * Sınıf Çalışma Gruplarını A4 PDF Krokisi Olarak Dışa Aktarır.
     */
    fun exportGroupListPdf(
        context: Context,
        classRoom: ClassRoom,
        groups: List<GroupTeam>,
        allStudents: List<Student>
    ): Uri {
        val pdfDocument = PdfDocument()
        // A4 Portrait: 595 x 842 points
        val pageWidth = 595
        val pageHeight = 842
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val paint = Paint().apply { isAntiAlias = true }

        val navyPrimary = Color.parseColor("#0F294A")
        val turquoise = Color.parseColor("#0D9488")
        val darkText = Color.parseColor("#1E293B")
        val mutedText = Color.parseColor("#64748B")

        // 1. ÜST HEADER
        val headerRect = RectF(24f, 20f, pageWidth - 24f, 85f)
        paint.color = Color.parseColor("#F8FAFC")
        canvas.drawRoundRect(headerRect, 8f, 8f, paint)

        paint.style = Paint.Style.STROKE
        paint.color = Color.parseColor("#E2E8F0")
        paint.strokeWidth = 1f
        canvas.drawRoundRect(headerRect, 8f, 8f, paint)
        paint.style = Paint.Style.FILL

        val emblem = renderEmblemBitmap(context, 54)
        if (emblem != null) {
            canvas.drawBitmap(emblem, 34f, 25f, paint)
        }

        paint.color = navyPrimary
        paint.textSize = 14f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("${classRoom.schoolName} • ${classRoom.gradeLevel}/${classRoom.section} SINIFI", 98f, 42f, paint)

        paint.color = turquoise
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("👥 AKILLI SINIF ÇALIŞMA GRUPLARI LİSTESİ", 98f, 59f, paint)

        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale("tr", "TR"))
        val dateStr = dateFormat.format(Date())
        paint.color = mutedText
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Öğretmen: ${classRoom.teacherName}  |  Tarih: $dateStr  |  Toplam Grup: ${groups.size}", 98f, 74f, paint)

        // 2. İSTATİSTİK ŞERİDİ
        val totalMembers = groups.sumOf { it.studentIds.size }
        val allAssignedStudents = groups.flatMap { it.studentIds }.mapNotNull { sid -> allStudents.firstOrNull { it.id == sid } }
        val girlCount = allAssignedStudents.count { it.gender.equals("KIZ", ignoreCase = true) }
        val boyCount = allAssignedStudents.count { it.gender.equals("ERKEK", ignoreCase = true) }

        val statRect = RectF(24f, 95f, pageWidth - 24f, 120f)
        paint.color = Color.parseColor("#F1F5F9")
        canvas.drawRoundRect(statRect, 6f, 6f, paint)

        paint.color = darkText
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val statSummary = "İstatistik: ${groups.size} Grup  •  $totalMembers Öğrenci  •  ♀ $girlCount Kız  •  ♂ $boyCount Erkek"
        canvas.drawText(statSummary, 34f, 111f, paint)

        // 3. GRUP KARTLARI IZGARASI (Grid: 2 Kolon)
        val startY = 130f
        val bottomMargin = 780f
        val colGap = 14f
        val rowGap = 14f
        val leftX = 24f
        val availableWidth = (pageWidth - 48f - colGap)
        val colWidth = availableWidth / 2f

        val columns = 2
        val maxCardsPerCol = ((groups.size + 1) / 2).coerceAtLeast(1)
        val availableHeight = bottomMargin - startY
        val cardHeight = ((availableHeight - (maxCardsPerCol - 1) * rowGap) / maxCardsPerCol).coerceIn(90f, 150f)

        groups.forEachIndexed { index, group ->
            val col = index % columns
            val row = index / columns

            val cardLeft = leftX + col * (colWidth + colGap)
            val cardTop = startY + row * (cardHeight + rowGap)
            val cardRect = RectF(cardLeft, cardTop, cardLeft + colWidth, cardTop + cardHeight)

            // Kart Arkaplanı
            val theme = group.colorTheme
            paint.color = try { Color.parseColor(theme.bgHex) } catch (e: Exception) { Color.WHITE }
            canvas.drawRoundRect(cardRect, 6f, 6f, paint)

            paint.style = Paint.Style.STROKE
            paint.color = try { Color.parseColor(theme.borderHex) } catch (e: Exception) { Color.LTGRAY }
            paint.strokeWidth = 1f
            canvas.drawRoundRect(cardRect, 6f, 6f, paint)
            paint.style = Paint.Style.FILL

            // Kart Başlığı Şeridi
            val headerHeight = 22f
            val cardHeaderRect = RectF(cardLeft, cardTop, cardLeft + colWidth, cardTop + headerHeight)
            paint.color = try { Color.parseColor(theme.primaryHex) } catch (e: Exception) { navyPrimary }
            canvas.drawRoundRect(cardHeaderRect, 6f, 6f, paint)
            // Alt köşeleri düzeltmek için düz dikdörtgen
            canvas.drawRect(RectF(cardLeft, cardTop + 10f, cardLeft + colWidth, cardTop + headerHeight), paint)

            val members = group.studentIds.mapNotNull { sid -> allStudents.firstOrNull { it.id == sid } }
            val groupGirls = members.count { it.gender.equals("KIZ", ignoreCase = true) }
            val groupBoys = members.count { it.gender.equals("ERKEK", ignoreCase = true) }

            paint.color = Color.WHITE
            paint.textSize = 9.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("📌 ${group.name.uppercase(Locale("tr", "TR"))}", cardLeft + 8f, cardTop + 15f, paint)

            val badgeText = "${members.size} Üye (${groupGirls}K/${groupBoys}E)"
            val btw = paint.measureText(badgeText)
            canvas.drawText(badgeText, cardLeft + colWidth - btw - 8f, cardTop + 15f, paint)

            // Öğrenci Listesi
            var currentStudentY = cardTop + 34f
            val maxVisible = 6
            members.take(maxVisible).forEachIndexed { mIdx, student ->
                val isLeader = student.id == group.leaderStudentId
                paint.color = if (isLeader) Color.parseColor("#B45309") else darkText
                paint.textSize = 8f
                paint.typeface = if (isLeader) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

                val prefix = "${mIdx + 1}. "
                val leaderTag = if (isLeader) " 👑 (Sözcü)" else ""
                val lineText = "$prefix#${student.studentNumber} ${student.fullName}$leaderTag"
                canvas.drawText(lineText, cardLeft + 8f, currentStudentY, paint)
                currentStudentY += 13f
            }

            if (members.size > maxVisible) {
                paint.color = mutedText
                paint.textSize = 7.5f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
                canvas.drawText("+ ${members.size - maxVisible} öğrenci daha...", cardLeft + 8f, currentStudentY, paint)
            }
        }

        // 4. ALT BİLGİ (FOOTER)
        val footerY = 810f
        paint.color = mutedText
        paint.textSize = 8f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("BİRİZ Akıllı Sınıf Platformu  •  Grup Oluşturma Modülü", 24f, footerY, paint)

        val noteText = "Her hakkı saklıdır. Pedagojik denge gözetilerek üretilmiştir."
        val ntw = paint.measureText(noteText)
        canvas.drawText(noteText, pageWidth - 24f - ntw, footerY, paint)

        pdfDocument.finishPage(page)

        val safeSchool = classRoom.schoolName.replace(" ", "_").filter { it.isLetterOrDigit() || it == '_' }
        val fileName = "${safeSchool}_${classRoom.gradeLevel}${classRoom.section}_Grup_Listesi.pdf"
        val file = File(context.cacheDir, fileName)

        FileOutputStream(file).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    /**
     * Sınıf Listesini WhatsApp / SMS için formatlı metin olarak üretir.
     */
    fun generateClassListShareText(classRoom: ClassRoom, students: List<Student>): String {
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale("tr", "TR"))
        val dateStr = dateFormat.format(Date())
        val sorted = students.sortedBy { it.studentNumber.toIntOrNull() ?: 9999 }
        val girlCount = sorted.count { it.gender.trim().equals("Kız", ignoreCase = true) }
        val boyCount = sorted.count { it.gender.trim().equals("Erkek", ignoreCase = true) }

        val sb = StringBuilder()
        sb.append("🏫 ${classRoom.schoolName} - ${classRoom.gradeLevel}/${classRoom.section} Sınıfı\n")
        sb.append("📋 Sınıf Öğrenci Listesi ($dateStr)\n")
        sb.append("👥 Toplam: ${sorted.size} Öğrenci (♀ $girlCount Kız, ♂ $boyCount Erkek)\n")
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━\n\n")

        sorted.forEachIndexed { index, s ->
            val genderIcon = if (s.gender.trim().equals("Kız", ignoreCase = true)) "♀" else "♂"
            sb.append("${index + 1}. No: ${s.studentNumber} - ${s.fullName} ($genderIcon)\n")
        }
        sb.append("\nBİRİZ Sınıf Yönetim Sistemi ✨")
        return sb.toString()
    }

    /**
     * Haftalık Ders Programını WhatsApp / SMS için formatlı metin olarak üretir.
     */
    fun generateWeeklyScheduleShareText(
        classRoom: ClassRoom,
        config: ScheduleConfig,
        lessons: List<ScheduleLesson>
    ): String {
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale("tr", "TR"))
        val dateStr = dateFormat.format(Date())

        val sb = StringBuilder()
        sb.append("🏫 ${classRoom.schoolName} - ${classRoom.gradeLevel}/${classRoom.section} Sınıfı\n")
        sb.append("📅 Haftalık Ders Programı ($dateStr)\n")
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━\n\n")

        val days = ScheduleHelper.DAYS_OF_WEEK
        days.forEach { dayKey ->
            val dayName = ScheduleHelper.DAY_NAMES[dayKey] ?: dayKey
            sb.append("📌 ${dayName.uppercase(Locale("tr", "TR"))}\n")

            val lessonsForDay = lessons.filter { it.dayOfWeek == dayKey }
            val timeline = ScheduleHelper.calculateTimeline(config, lessonsForDay)

            timeline.forEach { slot ->
                when (slot) {
                    is ScheduleTimelineItem.LessonSlot -> {
                        val subName = slot.lesson?.subjectName?.ifBlank { "Boş Ders" } ?: "Boş Ders"
                        sb.append("  ${slot.lessonIndex}. Ders (${slot.startTime} - ${slot.endTime}): $subName\n")
                    }
                    is ScheduleTimelineItem.LunchBreakSlot -> {
                        sb.append("  🍽️ Öğle Arası (${slot.startTime} - ${slot.endTime})\n")
                    }
                    is ScheduleTimelineItem.RecessSlot -> {
                        // Teneffüs bilgisi
                    }
                }
            }
            sb.append("\n")
        }
        sb.append("BİRİZ Sınıf Yönetim Sistemi ✨")
        return sb.toString()
    }

    /**
     * Haftalık Ders Programını Excel (.xlsx) olarak dışa aktarır.
     */
    fun exportWeeklyScheduleExcel(
        context: Context,
        classRoom: ClassRoom,
        config: ScheduleConfig,
        lessons: List<ScheduleLesson>
    ): Uri {
        val safeSchool = classRoom.schoolName.replace(" ", "_").filter { it.isLetterOrDigit() || it == '_' }
        val fileName = "${safeSchool}_${classRoom.gradeLevel}${classRoom.section}_Ders_Programi.xlsx"
        val file = File(context.cacheDir, fileName)

        val days = ScheduleHelper.DAYS_OF_WEEK
        val dayHeaders = days.map { ScheduleHelper.DAY_NAMES[it] ?: it }
        val headers = listOf("Ders / Saat") + dayHeaders

        val rows = mutableListOf<List<String>>()

        val sampleTimeline = ScheduleHelper.calculateTimeline(config, emptyList())
        val lessonSlots = sampleTimeline.filterIsInstance<ScheduleTimelineItem.LessonSlot>()

        lessonSlots.forEach { slot ->
            val row = mutableListOf<String>()
            row.add("${slot.lessonIndex}. Ders (${slot.startTime} - ${slot.endTime})")

            days.forEach { dayKey ->
                val match = lessons.firstOrNull { it.dayOfWeek == dayKey && it.lessonIndex == slot.lessonIndex }
                val name = match?.subjectName?.ifBlank { "Boş Ders" } ?: "Boş Ders"
                row.add(name)
            }
            rows.add(row)
        }

        writeXlsx(file, headers, rows)

        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    /**
     * Sınıf Oturma Planını WhatsApp / SMS için formatlı metin olarak üretir.
     */
    fun generateSeatingPlanShareText(
        classRoom: ClassRoom,
        seatingPlanState: SeatingPlanState,
        students: List<Student>
    ): String {
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale("tr", "TR"))
        val dateStr = dateFormat.format(Date())
        val seatedCount = seatingPlanState.assignments.values.filter { it.isNotBlank() }.distinct().size
        val config = seatingPlanState.config

        val sb = StringBuilder()
        sb.append("🏫 ${classRoom.schoolName} - ${classRoom.gradeLevel}/${classRoom.section} Sınıfı\n")
        sb.append("🪑 Sınıf Oturma Planı ($dateStr)\n")
        sb.append("👥 $seatedCount / ${students.size} Öğrenci Yerleşik (${config.totalSeats} Koltuk)\n")
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━\n\n")

        val groupCount = config.groupCount.coerceIn(1, 6)
        val peoplePerRow = config.peoplePerRow.coerceIn(1, 4)

        for (g in 0 until groupCount) {
            val rowsInGroup = config.rowsPerGroup.getOrElse(g) { 5 }
            sb.append("📌 ${g + 1}. GRUP ($rowsInGroup Sıra)\n")

            for (r in 0 until rowsInGroup) {
                sb.append("  • ${r + 1}. Sıra: ")
                val seatTexts = mutableListOf<String>()
                for (s in 0 until peoplePerRow) {
                    val seatId = "g$g-r$r-s$s"
                    val studentId = seatingPlanState.assignments[seatId]
                    val student = students.firstOrNull { it.id == studentId }
                    if (student != null) {
                        seatTexts.add("${student.studentNumber} ${student.fullName}")
                    } else {
                        seatTexts.add("(Boş)")
                    }
                }
                sb.append(seatTexts.joinToString(" | "))
                sb.append("\n")
            }
            sb.append("\n")
        }

        sb.append("BİRİZ Sınıf Yönetim Sistemi ✨")
        return sb.toString()
    }

    /**
     * Sınıf Oturma Planını Excel (.xlsx) olarak dışa aktarır.
     */
    fun exportSeatingPlanExcel(
        context: Context,
        classRoom: ClassRoom,
        seatingPlanState: SeatingPlanState,
        students: List<Student>
    ): Uri {
        val safeSchool = classRoom.schoolName.replace(" ", "_").filter { it.isLetterOrDigit() || it == '_' }
        val fileName = "${safeSchool}_${classRoom.gradeLevel}${classRoom.section}_Oturma_Plani.xlsx"
        val file = File(context.cacheDir, fileName)

        val headers = listOf("Grup", "Sıra No", "Koltuk No", "Öğrenci No", "Adı Soyadı", "Cinsiyet", "Durum")
        val rows = mutableListOf<List<String>>()

        val config = seatingPlanState.config
        val groupCount = config.groupCount.coerceIn(1, 6)
        val peoplePerRow = config.peoplePerRow.coerceIn(1, 4)

        for (g in 0 until groupCount) {
            val rowsInGroup = config.rowsPerGroup.getOrElse(g) { 5 }
            for (r in 0 until rowsInGroup) {
                for (s in 0 until peoplePerRow) {
                    val seatId = "g$g-r$r-s$s"
                    val studentId = seatingPlanState.assignments[seatId]
                    val student = students.firstOrNull { it.id == studentId }

                    if (student != null) {
                        rows.add(
                            listOf(
                                "${g + 1}. Grup",
                                "${r + 1}. Sıra",
                                "${s + 1}. Koltuk",
                                student.studentNumber,
                                student.fullName,
                                student.gender,
                                "Dolu"
                            )
                        )
                    } else {
                        rows.add(
                            listOf(
                                "${g + 1}. Grup",
                                "${r + 1}. Sıra",
                                "${s + 1}. Koltuk",
                                "-",
                                "-",
                                "-",
                                "Boş"
                            )
                        )
                    }
                }
            }
        }

        writeXlsx(file, headers, rows)

        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    /**
     * Oluşturulan dosyayı diğer uygulamalarla (WhatsApp, E-posta, Dosyalar, Yazdır vb.) paylaşır / açar.
     */
    fun shareFile(context: Context, uri: Uri, mimeType: String, chooserTitle: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, chooserTitle).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
