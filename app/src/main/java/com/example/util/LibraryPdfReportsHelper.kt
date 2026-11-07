package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.R
import com.example.data.model.Book
import com.example.data.model.ClassRoom
import com.example.data.model.ReadingRecord
import com.example.data.model.Student
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * BİRİZ Gelişmiş Kitaplık & Okuma Takip PDF Raporları Oluşturucu
 * 9 Farklı Resmi ve Kullanışlı A4 PDF Belgesi Üretir
 */
object LibraryPdfReportsHelper {

    private const val PAGE_WIDTH = 595  // A4 Dikey Genişlik (pt)
    private const val PAGE_HEIGHT = 842 // A4 Dikey Yükseklik (pt)

    private val tealTheme = 0xFF007A78.toInt()
    private val navyTheme = 0xFF13294B.toInt()
    private val darkText = 0xFF0F172A.toInt()
    private val mutedText = 0xFF64748B.toInt()
    private val tableBorderColor = 0xFF94A3B8.toInt()
    private val zebraColor = 0xFFF8FAFC.toInt()

    private fun renderEmblemBitmap(context: Context, sizePx: Int = 100): Bitmap? {
        return runCatching {
            val drawable = ContextCompat.getDrawable(context, R.drawable.ic_biriz_emblem) ?: return null
            val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, sizePx, sizePx)
            drawable.draw(canvas)
            bitmap
        }.getOrNull()
    }

    private fun sanitizeFileName(name: String): String {
        return name.replace(" ", "_").filter { it.isLetterOrDigit() || it == '_' }
    }

    private fun createPdfFile(context: Context, prefix: String, classRoom: ClassRoom): File {
        val safeSchool = sanitizeFileName(classRoom.schoolName)
        val fileName = "${prefix}_${safeSchool}_${classRoom.gradeLevel}${classRoom.section}_${System.currentTimeMillis()}.pdf"
        return File(context.cacheDir, fileName)
    }

    private fun getFileUri(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    private fun drawDocumentHeader(
        canvas: Canvas,
        context: Context,
        classRoom: ClassRoom,
        docTitle: String,
        subTitle: String? = null
    ): Float {
        val emblem = renderEmblemBitmap(context, 76)
        emblem?.let {
            canvas.drawBitmap(it, 36f, 30f, null)
        }

        val textStartX = if (emblem != null) 118f else 36f
        val schoolPaint = Paint().apply {
            color = navyTheme
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val titlePaint = Paint().apply {
            color = tealTheme
            textSize = 12.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val metaPaint = Paint().apply {
            color = mutedText
            textSize = 8.5f
            typeface = Typeface.DEFAULT
            isAntiAlias = true
        }

        val schoolHeader = "${classRoom.schoolName.uppercase(Locale("tr"))} - ${classRoom.gradeLevel}/${classRoom.section} SINIFI"
        canvas.drawText(schoolHeader, textStartX, 44f, schoolPaint)
        canvas.drawText(docTitle.uppercase(Locale("tr")), textStartX, 62f, titlePaint)

        val dateStr = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("tr")).format(Date())
        val teacherStr = if (classRoom.teacherName.isNotBlank()) "Öğretmen: ${classRoom.teacherName} | " else ""
        canvas.drawText("$teacherStr$dateStr ${subTitle?.let { " | $it" } ?: ""}", textStartX, 76f, metaPaint)

        val linePaint = Paint().apply {
            color = 0xFFCBD5E1.toInt()
            strokeWidth = 1f
        }
        canvas.drawLine(36f, 92f, (PAGE_WIDTH - 36).toFloat(), 92f, linePaint)
        return 106f
    }

    private fun drawFooter(canvas: Canvas, pageNum: Int, totalPages: Int = 1) {
        val footerPaint = Paint().apply {
            color = mutedText
            textSize = 8f
            isAntiAlias = true
        }
        val leftText = "BİRİZ Akıllı Sınıf & Okuma Takip Sistemi"
        val rightText = "Sayfa $pageNum / $totalPages"
        canvas.drawText(leftText, 36f, (PAGE_HEIGHT - 22).toFloat(), footerPaint)

        val rightWidth = footerPaint.measureText(rightText)
        canvas.drawText(rightText, PAGE_WIDTH - 36f - rightWidth, (PAGE_HEIGHT - 22).toFloat(), footerPaint)
    }

    private fun drawDefterFooter(canvas: Canvas, outerMargin: Float = 32f) {
        val footerPaint = Paint().apply {
            color = mutedText
            textSize = 8.5f
            typeface = Typeface.DEFAULT
            isAntiAlias = true
        }
        val leftText = "BİRİZ Akıllı Sınıf & Okuma Takip Sistemi"
        val yPos = (PAGE_HEIGHT - 22).toFloat()
        canvas.drawText(leftText, outerMargin, yPos, footerPaint)

        // Sağ Kısım: "Sayfa " + İçi Boş Daire (Öğretmenin sayfa no yazabilmesi için)
        val rightLabel = "Sayfa "
        val labelWidth = footerPaint.measureText(rightLabel)
        val circleRadius = 7f
        val circleMargin = 4f
        val totalBlockWidth = labelWidth + circleMargin + circleRadius * 2

        val startX = PAGE_WIDTH - outerMargin - totalBlockWidth
        canvas.drawText(rightLabel, startX, yPos, footerPaint)

        val circlePaint = Paint().apply {
            color = mutedText
            style = Paint.Style.STROKE
            strokeWidth = 1f
            isAntiAlias = true
        }
        val circleCenterX = startX + labelWidth + circleMargin + circleRadius
        val circleCenterY = yPos - 3.5f
        canvas.drawCircle(circleCenterX, circleCenterY, circleRadius, circlePaint)
    }

    // ---------------------------------------------------------------------------------------------
    // 1. KİTAPLIK LİSTESİ (PDF ve Kitap No Sıralı)
    // ---------------------------------------------------------------------------------------------
    fun exportBooksListPdf(context: Context, classRoom: ClassRoom, books: List<Book>): Uri {
        val file = createPdfFile(context, "Kitaplik_Listesi", classRoom)
        val pdf = PdfDocument()
        val sortedBooks = books.sortedBy { it.registrationNo }
        val itemsPerPage = 32
        val totalPages = ((sortedBooks.size + itemsPerPage - 1) / itemsPerPage).coerceAtLeast(1)

        val thBgPaint = Paint().apply { color = navyTheme; style = Paint.Style.FILL }
        val thTextPaint = Paint().apply { color = Color.WHITE; textSize = 9f; typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true }
        val tdTextPaint = Paint().apply { color = darkText; textSize = 8.5f; isAntiAlias = true }
        val zebraPaint = Paint().apply { color = zebraColor; style = Paint.Style.FILL }
        val linePaint = Paint().apply { color = 0xFFE2E8F0.toInt(); strokeWidth = 0.6f }

        for (p in 0 until totalPages) {
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, p + 1).create()
            val page = pdf.startPage(pageInfo)
            val canvas = page.canvas

            var y = drawDocumentHeader(canvas, context, classRoom, "Sınıf Kitaplığı Demirbaş Listesi", "Toplam ${books.size} Kitap")

            // Table Header
            canvas.drawRect(36f, y, (PAGE_WIDTH - 36).toFloat(), y + 20f, thBgPaint)
            canvas.drawText("No", 42f, y + 14f, thTextPaint)
            canvas.drawText("Kitap Adı", 85f, y + 14f, thTextPaint)
            canvas.drawText("Yazar", 280f, y + 14f, thTextPaint)
            canvas.drawText("Sayfa", 410f, y + 14f, thTextPaint)
            canvas.drawText("Durum", 460f, y + 14f, thTextPaint)
            canvas.drawText("Okuyan Öğrenci", 515f, y + 14f, thTextPaint)
            y += 20f

            val startIndex = p * itemsPerPage
            val endIndex = (startIndex + itemsPerPage).coerceAtMost(sortedBooks.size)

            for (i in startIndex until endIndex) {
                val b = sortedBooks[i]
                if (i % 2 == 1) {
                    canvas.drawRect(36f, y, (PAGE_WIDTH - 36).toFloat(), y + 18f, zebraPaint)
                }
                canvas.drawLine(36f, y + 18f, (PAGE_WIDTH - 36).toFloat(), y + 18f, linePaint)

                canvas.drawText("#${b.registrationNo}", 42f, y + 13f, tdTextPaint)
                val nameCut = if (b.name.length > 36) b.name.take(34) + ".." else b.name
                canvas.drawText(nameCut, 85f, y + 13f, tdTextPaint)

                val authorCut = if (b.author.length > 22) b.author.take(20) + ".." else b.author.ifBlank { "-" }
                canvas.drawText(authorCut, 280f, y + 13f, tdTextPaint)

                canvas.drawText(if (b.pageCount > 0) "${b.pageCount} s." else "-", 410f, y + 13f, tdTextPaint)

                val isReading = b.status == "Okunuyor" || b.activeAssignments.isNotEmpty()
                val statusText = if (isReading) "Okunuyor" else "Rafta"
                canvas.drawText(statusText, 460f, y + 13f, tdTextPaint)

                val reader = when {
                    b.activeAssignments.isNotEmpty() -> b.activeAssignments.first().studentName
                    !b.currentStudentName.isNullOrBlank() -> b.currentStudentName
                    else -> "-"
                }
                val readerCut = if (reader.length > 12) reader.take(10) + ".." else reader
                canvas.drawText(readerCut, 515f, y + 13f, tdTextPaint)

                y += 18f
            }

            drawFooter(canvas, p + 1, totalPages)
            pdf.finishPage(page)
        }

        FileOutputStream(file).use { pdf.writeTo(it) }
        pdf.close()
        return getFileUri(context, file)
    }

    // ---------------------------------------------------------------------------------------------
    // 2. SINIF KİTAP OKUMA TAKİP DEFTERİ (PDF - Şablon Birebir Uyarlandı)
    // Öğrenci isimleri yazılmaz, boş bırakılır; her öğrenci kendi adını yazar.
    // ---------------------------------------------------------------------------------------------
    fun exportClassroomReadingTrackerSheetPdf(context: Context, classRoom: ClassRoom): Uri {
        val file = createPdfFile(context, "Sinif_Kitap_Okuma_Takip_Defteri", classRoom)
        val pdf = PdfDocument()

        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = pdf.startPage(pageInfo)
        val canvas = page.canvas

        drawCustomDefterSheet(
            canvas = canvas,
            context = context,
            classRoom = classRoom,
            isIndividual = false,
            studentNameTitle = null,
            pageNumber = 1,
            totalPages = 1
        )

        pdf.finishPage(page)
        FileOutputStream(file).use { pdf.writeTo(it) }
        pdf.close()
        return getFileUri(context, file)
    }

    // ---------------------------------------------------------------------------------------------
    // 3. BİREYSEL KİTAP OKUMA TAKİP DEFTERİ (PDF - Her Öğrenciye Özel Sayfa)
    // ---------------------------------------------------------------------------------------------
    fun exportIndividualReadingTrackerDefterPdf(
        context: Context,
        classRoom: ClassRoom,
        students: List<Student>
    ): Uri {
        val file = createPdfFile(context, "Bireysel_Kitap_Okuma_Takip_Defteri", classRoom)
        val pdf = PdfDocument()
        val sortedStudents = students.sortedBy { it.studentNumber.toIntOrNull() ?: 999 }
        val totalPages = sortedStudents.size.coerceAtLeast(1)

        sortedStudents.forEachIndexed { idx, st ->
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, idx + 1).create()
            val page = pdf.startPage(pageInfo)
            val canvas = page.canvas

            val stLabel = "${st.fullName} (#${st.studentNumber})"
            drawCustomDefterSheet(
                canvas = canvas,
                context = context,
                classRoom = classRoom,
                isIndividual = true,
                studentNameTitle = stLabel,
                pageNumber = idx + 1,
                totalPages = totalPages
            )

            pdf.finishPage(page)
        }

        FileOutputStream(file).use { pdf.writeTo(it) }
        pdf.close()
        return getFileUri(context, file)
    }

    /**
     * Kullanıcının eklediği PDF şablonuna birebir sadık kalarak A4 tablosu çizen çekirdek motor.
     */
    private fun drawCustomDefterSheet(
        canvas: Canvas,
        context: Context,
        classRoom: ClassRoom,
        isIndividual: Boolean,
        studentNameTitle: String?,
        pageNumber: Int,
        totalPages: Int
    ) {
        val outerMargin = 32f
        val tableRight = PAGE_WIDTH - outerMargin
        val headerTop = 32f
        val headerBottom = 96f
        val headerHeight = headerBottom - headerTop

        val borderPaint = Paint().apply {
            color = tealTheme
            style = Paint.Style.STROKE
            strokeWidth = 1.6f
            isAntiAlias = true
        }
        val gridLinePaint = Paint().apply {
            color = tableBorderColor
            style = Paint.Style.STROKE
            strokeWidth = 0.8f
            isAntiAlias = true
        }

        // 1. Üst Başlık Kutusu (Header Box)
        canvas.drawRect(outerMargin, headerTop, tableRight, headerBottom, borderPaint)

        // Dikey bölücüler: Bireysel defterde Ay/Yıl kutusu yok (kullanıcı talebi)
        val col1Divider: Float
        val col2Divider: Float?

        if (isIndividual) {
            // Sadece Sınıf / Şube kutusu var (Genişlik: 90 pt)
            col1Divider = tableRight - 90f
            col2Divider = null
            canvas.drawLine(col1Divider, headerTop, col1Divider, headerBottom, borderPaint)
        } else {
            // Sınıf / Şube kutusu + Ay / Yıl kutusu
            col1Divider = tableRight - 150f
            col2Divider = tableRight - 75f
            canvas.drawLine(col1Divider, headerTop, col1Divider, headerBottom, borderPaint)
            canvas.drawLine(col2Divider, headerTop, col2Divider, headerBottom, borderPaint)
        }

        // Sol Kısım: Amblem + Okul Adı + Başlık (+ Öğrenci)
        val emblemSize = 48f
        val emblemX = outerMargin + 10f
        val emblemY = headerTop + (headerHeight - emblemSize) / 2f
        val emblem = renderEmblemBitmap(context, emblemSize.toInt())
        emblem?.let {
            canvas.drawBitmap(it, emblemX, emblemY, null)
        }

        // Amblem ile metin arasında net ve ferah boşluk (asla üst üste binmez)
        val textStartX = if (emblem != null) emblemX + emblemSize + 12f else outerMargin + 14f

        val schoolPaint = Paint().apply {
            color = tealTheme
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val docTitlePaint = Paint().apply {
            color = navyTheme
            textSize = 10.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val studentPaint = Paint().apply {
            color = 0xFFD97706.toInt()
            textSize = 10.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val schoolName = classRoom.schoolName.ifBlank { "T.C. MİLLİ EĞİTİM BAKANLIĞI" }.uppercase(Locale("tr"))
        // Okul ismi çok uzunsa bölücüye taşmaması için font boyutunu dinamik sığdır
        val maxSchoolWidth = col1Divider - textStartX - 10f
        while (schoolPaint.measureText(schoolName) > maxSchoolWidth && schoolPaint.textSize > 8f) {
            schoolPaint.textSize -= 0.5f
        }

        if (isIndividual) {
            canvas.drawText(schoolName, textStartX, headerTop + 22f, schoolPaint)
            canvas.drawText("BİREYSEL KİTAP OKUMA TAKİP DEFTERİ", textStartX, headerTop + 39f, docTitlePaint)
            if (!studentNameTitle.isNullOrBlank()) {
                canvas.drawText("ÖĞRENCİ: $studentNameTitle", textStartX, headerTop + 55f, studentPaint)
            }
        } else {
            canvas.drawText(schoolName, textStartX, headerTop + 28f, schoolPaint)
            canvas.drawText("KİTAP OKUMA TAKİP DEFTERİ", textStartX, headerTop + 48f, docTitlePaint)
        }

        val metaLabelPaint = Paint().apply {
            color = mutedText
            textSize = 7.5f
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }
        val metaValPaint = Paint().apply {
            color = navyTheme
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        if (isIndividual) {
            // Sınıf / Şube (Orta-Sağ)
            canvas.drawText("SINIF / ŞUBE", col1Divider + 14f, headerTop + 22f, metaLabelPaint)
            canvas.drawText("${classRoom.gradeLevel} / ${classRoom.section}", col1Divider + 22f, headerTop + 48f, metaValPaint)
        } else {
            // Sınıf / Şube (Orta)
            canvas.drawText("SINIF /", col1Divider + 14f, headerTop + 18f, metaLabelPaint)
            canvas.drawText("ŞUBE", col1Divider + 16f, headerTop + 28f, metaLabelPaint)
            canvas.drawText("${classRoom.gradeLevel} / ${classRoom.section}", col1Divider + 16f, headerTop + 52f, metaValPaint)

            // Ay / Yıl (Sağ) - İçeriği boş bırakılır, öğretmen doldurur
            col2Divider?.let { div ->
                canvas.drawText("AY / YIL", div + 14f, headerTop + 22f, metaLabelPaint)
                // İçerik boş bırakıldı (öğretmen dolduracak)
            }
        }

        // 2. Tablo Başlıkları (Column Headers)
        val tableTop = 112f
        val tableHeaderHeight = 36f
        val tableHeaderBottom = tableTop + tableHeaderHeight

        val thPaint = Paint().apply {
            color = navyTheme
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val subDatePaint = Paint().apply {
            color = mutedText
            textSize = 6.8f
            typeface = Typeface.DEFAULT
            isAntiAlias = true
        }

        val c0 = outerMargin
        val cEnd = tableRight

        if (isIndividual) {
            // Bireysel Defter Sütunları:
            // "Öğrenci / İmza" sütunu kaldırıldı, alanı "Kitabın Adı" sütununa aktarıldı (306 pt!)
            val c1 = c0 + 35f   // Sıra No (35 pt)
            val c2 = c1 + 306f  // Kitabın Adı (306 pt geniş alan!)
            val c3 = c2 + 46f   // Sayfa Sayısı (46 pt)
            val c4 = c3 + 52f   // Başlama Tarihi (52 pt)
            val c5 = c4 + 52f   // Bitiş Tarihi (52 pt)
            val c6 = cEnd       // Sisteme İşlendi mi? (40 pt)

            // Dış çerçeve ve dikey çizgiler
            canvas.drawRect(c0, tableTop, c6, tableHeaderBottom, borderPaint)
            listOf(c1, c2, c3, c4, c5).forEach { x ->
                canvas.drawLine(x, tableTop, x, tableHeaderBottom, borderPaint)
            }

            canvas.drawText("Sıra", c0 + 8f, tableTop + 16f, thPaint)
            canvas.drawText("No", c0 + 10f, tableTop + 27f, thPaint)

            canvas.drawText("Kitabın Adı", c1 + 115f, tableTop + 22f, thPaint)

            canvas.drawText("Sayfa", c2 + 9f, tableTop + 16f, thPaint)
            canvas.drawText("Sayısı", c2 + 8f, tableTop + 27f, thPaint)

            canvas.drawText("Başlama", c3 + 7f, tableTop + 13f, thPaint)
            canvas.drawText("Tarihi", c3 + 11f, tableTop + 23f, thPaint)
            canvas.drawText("(Gün / Ay)", c3 + 4f, tableTop + 32f, subDatePaint)

            canvas.drawText("Bitiş", c4 + 13f, tableTop + 13f, thPaint)
            canvas.drawText("Tarihi", c4 + 11f, tableTop + 23f, thPaint)
            canvas.drawText("(Gün / Ay)", c4 + 4f, tableTop + 32f, subDatePaint)

            canvas.drawText("Sisteme", c5 + 4f, tableTop + 13f, thPaint)
            canvas.drawText("İşlendi", c5 + 5f, tableTop + 23f, thPaint)
            canvas.drawText("mi?", c5 + 12f, tableTop + 32f, thPaint)

            // 3. Tablo Satırları (Tam 20 Satır)
            val rowCount = 20
            val rowHeight = 31f
            var curY = tableHeaderBottom

            val noPaint = Paint().apply {
                color = 0xFF007A78.toInt()
                textSize = 9.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            val checkboxPaint = Paint().apply {
                color = 0xFF94A3B8.toInt()
                style = Paint.Style.STROKE
                strokeWidth = 1f
                isAntiAlias = true
            }

            for (r in 1..rowCount) {
                val nextY = curY + rowHeight

                canvas.drawLine(c0, nextY, c6, nextY, if (r == rowCount) borderPaint else gridLinePaint)
                listOf(c0, c1, c2, c3, c4, c5, c6).forEach { x ->
                    canvas.drawLine(x, curY, x, nextY, if (x == c0 || x == c6) borderPaint else gridLinePaint)
                }

                val noStr = if (r < 10) "0$r" else "$r"
                canvas.drawText(noStr, c0 + 10f, curY + 20f, noPaint)

                val boxSize = 11f
                val boxX = c5 + (c6 - c5 - boxSize) / 2f
                val boxY = curY + (rowHeight - boxSize) / 2f
                canvas.drawRect(boxX, boxY, boxX + boxSize, boxY + boxSize, checkboxPaint)

                curY = nextY
            }

            canvas.drawRect(c0, tableTop, c6, curY, borderPaint)

        } else {
            // Sınıf Defteri Sütunları (7 Sütun)
            val c1 = c0 + 35f   // Sıra No (35 pt)
            val c2 = c1 + 130f  // Öğrenci Adı Soyadı (130 pt)
            val c3 = c2 + 176f  // Kitabın Adı (176 pt)
            val c4 = c3 + 46f   // Sayfa Sayısı (46 pt)
            val c5 = c4 + 52f   // Başlama Tarihi (52 pt)
            val c6 = c5 + 52f   // Bitiş Tarihi (52 pt)
            val c7 = cEnd       // Sisteme İşlendi mi? (40 pt)

            // Dış çerçeve ve dikey çizgiler
            canvas.drawRect(c0, tableTop, c7, tableHeaderBottom, borderPaint)
            listOf(c1, c2, c3, c4, c5, c6).forEach { x ->
                canvas.drawLine(x, tableTop, x, tableHeaderBottom, borderPaint)
            }

            canvas.drawText("Sıra", c0 + 8f, tableTop + 16f, thPaint)
            canvas.drawText("No", c0 + 10f, tableTop + 27f, thPaint)

            canvas.drawText("Öğrenci Adı Soyadı", c1 + 14f, tableTop + 22f, thPaint)

            canvas.drawText("Kitabın Adı", c2 + 45f, tableTop + 22f, thPaint)

            canvas.drawText("Sayfa", c3 + 9f, tableTop + 16f, thPaint)
            canvas.drawText("Sayısı", c3 + 8f, tableTop + 27f, thPaint)

            canvas.drawText("Başlama", c4 + 7f, tableTop + 13f, thPaint)
            canvas.drawText("Tarihi", c4 + 11f, tableTop + 23f, thPaint)
            canvas.drawText("(Gün / Ay)", c4 + 4f, tableTop + 32f, subDatePaint)

            canvas.drawText("Bitiş", c5 + 13f, tableTop + 13f, thPaint)
            canvas.drawText("Tarihi", c5 + 11f, tableTop + 23f, thPaint)
            canvas.drawText("(Gün / Ay)", c5 + 4f, tableTop + 32f, subDatePaint)

            canvas.drawText("Sisteme", c6 + 4f, tableTop + 13f, thPaint)
            canvas.drawText("İşlendi", c6 + 5f, tableTop + 23f, thPaint)
            canvas.drawText("mi?", c6 + 12f, tableTop + 32f, thPaint)

            // 3. Tablo Satırları (Tam 20 Satır)
            val rowCount = 20
            val rowHeight = 31f
            var curY = tableHeaderBottom

            val noPaint = Paint().apply {
                color = 0xFF007A78.toInt()
                textSize = 9.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            val checkboxPaint = Paint().apply {
                color = 0xFF94A3B8.toInt()
                style = Paint.Style.STROKE
                strokeWidth = 1f
                isAntiAlias = true
            }

            for (r in 1..rowCount) {
                val nextY = curY + rowHeight

                canvas.drawLine(c0, nextY, c7, nextY, if (r == rowCount) borderPaint else gridLinePaint)
                listOf(c0, c1, c2, c3, c4, c5, c6, c7).forEach { x ->
                    canvas.drawLine(x, curY, x, nextY, if (x == c0 || x == c7) borderPaint else gridLinePaint)
                }

                val noStr = if (r < 10) "0$r" else "$r"
                canvas.drawText(noStr, c0 + 10f, curY + 20f, noPaint)

                val boxSize = 11f
                val boxX = c6 + (c7 - c6 - boxSize) / 2f
                val boxY = curY + (rowHeight - boxSize) / 2f
                canvas.drawRect(boxX, boxY, boxX + boxSize, boxY + boxSize, checkboxPaint)

                curY = nextY
            }

            canvas.drawRect(c0, tableTop, c7, curY, borderPaint)
        }

        // Alt Bilgi: Sayfa yazısı ve öğretmenin sayfa numarası yazabilmesi için içi boş daire
        drawDefterFooter(canvas, outerMargin)
    }

    // ---------------------------------------------------------------------------------------------
    // 4. OKUMA & İADE LİSTESİ (PDF ve Tarihsel Olarak Eskiden Yeniye Sıralı)
    // ---------------------------------------------------------------------------------------------
    fun exportReadAndReturnHistoryPdf(
        context: Context,
        classRoom: ClassRoom,
        records: List<ReadingRecord>
    ): Uri {
        val file = createPdfFile(context, "Okuma_ve_Iade_Gecmisi", classRoom)
        val pdf = PdfDocument()
        val sortedRecords = records.sortedBy { it.endDate } // Eskiden yeniye doğru kronolojik
        val itemsPerPage = 30
        val totalPages = ((sortedRecords.size + itemsPerPage - 1) / itemsPerPage).coerceAtLeast(1)

        val thBgPaint = Paint().apply { color = navyTheme; style = Paint.Style.FILL }
        val thTextPaint = Paint().apply { color = Color.WHITE; textSize = 9f; typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true }
        val tdTextPaint = Paint().apply { color = darkText; textSize = 8.5f; isAntiAlias = true }
        val zebraPaint = Paint().apply { color = zebraColor; style = Paint.Style.FILL }
        val linePaint = Paint().apply { color = 0xFFE2E8F0.toInt(); strokeWidth = 0.6f }

        for (p in 0 until totalPages) {
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, p + 1).create()
            val page = pdf.startPage(pageInfo)
            val canvas = page.canvas

            var y = drawDocumentHeader(canvas, context, classRoom, "Kitap Okuma ve İade Hareketleri Listesi", "Tarihe Göre Eskiden Yeniye Sıralı")

            canvas.drawRect(36f, y, (PAGE_WIDTH - 36).toFloat(), y + 20f, thBgPaint)
            canvas.drawText("Sıra", 42f, y + 14f, thTextPaint)
            canvas.drawText("Öğrenci Adı Soyadı", 75f, y + 14f, thTextPaint)
            canvas.drawText("Okuduğu Kitap", 230f, y + 14f, thTextPaint)
            canvas.drawText("Sayfa", 380f, y + 14f, thTextPaint)
            canvas.drawText("Başlama", 425f, y + 14f, thTextPaint)
            canvas.drawText("İade Tarihi", 480f, y + 14f, thTextPaint)
            canvas.drawText("Yıldız", 535f, y + 14f, thTextPaint)
            y += 20f

            val startIndex = p * itemsPerPage
            val endIndex = (startIndex + itemsPerPage).coerceAtMost(sortedRecords.size)

            for (i in startIndex until endIndex) {
                val r = sortedRecords[i]
                if (i % 2 == 1) {
                    canvas.drawRect(36f, y, (PAGE_WIDTH - 36).toFloat(), y + 18f, zebraPaint)
                }
                canvas.drawLine(36f, y + 18f, (PAGE_WIDTH - 36).toFloat(), y + 18f, linePaint)

                canvas.drawText("${i + 1}", 42f, y + 13f, tdTextPaint)
                val stCut = if (r.studentName.length > 22) r.studentName.take(20) + ".." else r.studentName
                canvas.drawText(stCut, 75f, y + 13f, tdTextPaint)

                val bookCut = if (r.bookName.length > 26) r.bookName.take(24) + ".." else r.bookName
                canvas.drawText(bookCut, 230f, y + 13f, tdTextPaint)

                canvas.drawText("${r.pageCount} s.", 380f, y + 13f, tdTextPaint)

                val startStr = SimpleDateFormat("dd.MM.yy", Locale("tr")).format(Date(r.startDate))
                val endStr = SimpleDateFormat("dd.MM.yy", Locale("tr")).format(Date(r.endDate))
                canvas.drawText(startStr, 425f, y + 13f, tdTextPaint)
                canvas.drawText(endStr, 480f, y + 13f, tdTextPaint)

                canvas.drawText("+${r.earnedStars} ⭐", 535f, y + 13f, tdTextPaint)
                y += 18f
            }

            drawFooter(canvas, p + 1, totalPages)
            pdf.finishPage(page)
        }

        FileOutputStream(file).use { pdf.writeTo(it) }
        pdf.close()
        return getFileUri(context, file)
    }

    // ---------------------------------------------------------------------------------------------
    // 5. ÖĞRENCİNİN OKUDUĞU KİTAPLAR LİSTESİ (PDF ve Öğrenci Bazında Eskiden Yeniye)
    // ---------------------------------------------------------------------------------------------
    fun exportStudentReadingLogsPdf(
        context: Context,
        classRoom: ClassRoom,
        records: List<ReadingRecord>,
        students: List<Student>
    ): Uri {
        val file = createPdfFile(context, "Ogrencinin_Okudugu_Kitaplar_Listesi", classRoom)
        val pdf = PdfDocument()

        val recordsByStudent = records.groupBy { it.studentId }
        val sortedStudents = students.sortedBy { it.studentNumber.toIntOrNull() ?: 999 }

        var globalPage = 1

        val thBgPaint = Paint().apply { color = navyTheme; style = Paint.Style.FILL }
        val thTextPaint = Paint().apply { color = Color.WHITE; textSize = 9f; typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true }
        val tdTextPaint = Paint().apply { color = darkText; textSize = 8.5f; isAntiAlias = true }
        val zebraPaint = Paint().apply { color = zebraColor; style = Paint.Style.FILL }
        val sectionPaint = Paint().apply { color = tealTheme; textSize = 11f; typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true }

        // Her öğrenciyi veya öğrenci gruplarını sayfalandır
        sortedStudents.forEach { st ->
            val stRecords = recordsByStudent[st.id]?.sortedBy { it.endDate } ?: emptyList()

            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, globalPage).create()
            val page = pdf.startPage(pageInfo)
            val canvas = page.canvas

            var y = drawDocumentHeader(canvas, context, classRoom, "Öğrenci Kitap Okuma Geçmişi", "${st.fullName} (#${st.studentNumber})")

            // Öğrenci Özet Kutusu
            val totalBooks = stRecords.size
            val totalPages = stRecords.sumOf { it.pageCount }
            val totalStars = stRecords.sumOf { it.earnedStars }

            canvas.drawText("👤 Öğrenci: ${st.fullName}  |  Okunan Kitap: $totalBooks Adet  |  Toplam: $totalPages Sayfa  |  Kazanılan: +$totalStars ⭐", 36f, y, sectionPaint)
            y += 16f

            if (stRecords.isEmpty()) {
                canvas.drawText("Henüz tamamlanmış kitap okuma kaydı bulunmuyor.", 36f, y + 20f, tdTextPaint)
            } else {
                canvas.drawRect(36f, y, (PAGE_WIDTH - 36).toFloat(), y + 20f, thBgPaint)
                canvas.drawText("Sıra", 42f, y + 14f, thTextPaint)
                canvas.drawText("Kitabın Adı", 80f, y + 14f, thTextPaint)
                canvas.drawText("Sayfa", 330f, y + 14f, thTextPaint)
                canvas.drawText("Başlama", 380f, y + 14f, thTextPaint)
                canvas.drawText("Bitiş / İade", 440f, y + 14f, thTextPaint)
                canvas.drawText("Yıldız", 515f, y + 14f, thTextPaint)
                y += 20f

                stRecords.take(30).forEachIndexed { i, r ->
                    if (i % 2 == 1) {
                        canvas.drawRect(36f, y, (PAGE_WIDTH - 36).toFloat(), y + 18f, zebraPaint)
                    }
                    canvas.drawText("${i + 1}", 42f, y + 13f, tdTextPaint)
                    val bookCut = if (r.bookName.length > 36) r.bookName.take(34) + ".." else r.bookName
                    canvas.drawText(bookCut, 80f, y + 13f, tdTextPaint)
                    canvas.drawText("${r.pageCount} s.", 330f, y + 13f, tdTextPaint)

                    val startStr = SimpleDateFormat("dd.MM.yyyy", Locale("tr")).format(Date(r.startDate))
                    val endStr = SimpleDateFormat("dd.MM.yyyy", Locale("tr")).format(Date(r.endDate))
                    canvas.drawText(startStr, 380f, y + 13f, tdTextPaint)
                    canvas.drawText(endStr, 440f, y + 13f, tdTextPaint)
                    canvas.drawText("+${r.earnedStars} ⭐", 515f, y + 13f, tdTextPaint)
                    y += 18f
                }
            }

            drawFooter(canvas, globalPage, sortedStudents.size)
            pdf.finishPage(page)
            globalPage++
        }

        FileOutputStream(file).use { pdf.writeTo(it) }
        pdf.close()
        return getFileUri(context, file)
    }

    // ---------------------------------------------------------------------------------------------
    // 6. KİTABI OKUYAN ÖĞRENCİLER LİSTESİ (PDF ve Kitap Bazında Eskiden Yeniye)
    // ---------------------------------------------------------------------------------------------
    fun exportBookReadersLogPdf(
        context: Context,
        classRoom: ClassRoom,
        books: List<Book>,
        records: List<ReadingRecord>
    ): Uri {
        val file = createPdfFile(context, "Kitabi_Okuyan_Ogrenciler_Listesi", classRoom)
        val pdf = PdfDocument()

        val recordsByBook = records.groupBy { it.bookId.ifBlank { it.bookName.trim().lowercase(Locale("tr")) } }
        val sortedBooks = books.sortedBy { it.registrationNo }

        val thBgPaint = Paint().apply { color = navyTheme; style = Paint.Style.FILL }
        val thTextPaint = Paint().apply { color = Color.WHITE; textSize = 9f; typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true }
        val tdTextPaint = Paint().apply { color = darkText; textSize = 8.5f; isAntiAlias = true }
        val zebraPaint = Paint().apply { color = zebraColor; style = Paint.Style.FILL }

        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = pdf.startPage(pageInfo)
        val canvas = page.canvas

        var y = drawDocumentHeader(canvas, context, classRoom, "Kitabı Okuyan Öğrenciler Listesi", "Kitap Bazında Okuma Dökümü")

        canvas.drawRect(36f, y, (PAGE_WIDTH - 36).toFloat(), y + 20f, thBgPaint)
        canvas.drawText("No", 42f, y + 14f, thTextPaint)
        canvas.drawText("Kitap Adı", 75f, y + 14f, thTextPaint)
        canvas.drawText("Okuyan Öğrenciler (Tarihsel Sıralı)", 260f, y + 14f, thTextPaint)
        canvas.drawText("Toplam", 520f, y + 14f, thTextPaint)
        y += 20f

        sortedBooks.take(32).forEachIndexed { idx, book ->
            val key = book.id.ifBlank { book.name.trim().lowercase(Locale("tr")) }
            val readList = recordsByBook[key]?.sortedBy { it.endDate } ?: emptyList()

            if (idx % 2 == 1) {
                canvas.drawRect(36f, y, (PAGE_WIDTH - 36).toFloat(), y + 18f, zebraPaint)
            }

            canvas.drawText("#${book.registrationNo}", 42f, y + 13f, tdTextPaint)
            val nameCut = if (book.name.length > 24) book.name.take(22) + ".." else book.name
            canvas.drawText(nameCut, 75f, y + 13f, tdTextPaint)

            val readersSummary = if (readList.isNotEmpty()) {
                readList.joinToString(", ") { "${it.studentName} (${SimpleDateFormat("dd.MM", Locale("tr")).format(Date(it.endDate))})" }
            } else "-"
            val summaryCut = if (readersSummary.length > 44) readersSummary.take(42) + ".." else readersSummary
            canvas.drawText(summaryCut, 260f, y + 13f, tdTextPaint)

            canvas.drawText("${readList.size} kez", 520f, y + 13f, tdTextPaint)
            y += 18f
        }

        drawFooter(canvas, 1, 1)
        pdf.finishPage(page)

        FileOutputStream(file).use { pdf.writeTo(it) }
        pdf.close()
        return getFileUri(context, file)
    }

    // ---------------------------------------------------------------------------------------------
    // 7. EN ÇOK KİTAP OKUYAN ÖĞRENCİLER LİSTESİ (PDF)
    // ---------------------------------------------------------------------------------------------
    fun exportTopBookReadersPdf(
        context: Context,
        classRoom: ClassRoom,
        records: List<ReadingRecord>,
        students: List<Student>
    ): Uri {
        val file = createPdfFile(context, "En_Cok_Kitap_Okuyan_Ogrenciler", classRoom)
        val pdf = PdfDocument()

        val ranking = students.map { st ->
            val stRecords = records.filter { it.studentId == st.id }
            Triple(st, stRecords.size, stRecords.sumOf { it.pageCount })
        }.sortedByDescending { it.second } // Okunan kitap sayısına göre azalan

        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = pdf.startPage(pageInfo)
        val canvas = page.canvas

        var y = drawDocumentHeader(canvas, context, classRoom, "En Çok Kitap Okuyan Öğrenciler Listesi", "Kitap Adedine Göre Başarı Sıralaması")

        val thBgPaint = Paint().apply { color = navyTheme; style = Paint.Style.FILL }
        val thTextPaint = Paint().apply { color = Color.WHITE; textSize = 9f; typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true }
        val tdTextPaint = Paint().apply { color = darkText; textSize = 8.5f; isAntiAlias = true }
        val zebraPaint = Paint().apply { color = zebraColor; style = Paint.Style.FILL }
        val medalPaint = Paint().apply { color = 0xFFD97706.toInt(); textSize = 9.5f; typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true }

        canvas.drawRect(36f, y, (PAGE_WIDTH - 36).toFloat(), y + 20f, thBgPaint)
        canvas.drawText("Derece", 42f, y + 14f, thTextPaint)
        canvas.drawText("Öğrenci Adı Soyadı", 100f, y + 14f, thTextPaint)
        canvas.drawText("Okul No", 280f, y + 14f, thTextPaint)
        canvas.drawText("Okunan Kitap", 370f, y + 14f, thTextPaint)
        canvas.drawText("Toplam Sayfa", 480f, y + 14f, thTextPaint)
        y += 20f

        ranking.forEachIndexed { i, (st, bookCount, pageCount) ->
            if (i % 2 == 1) {
                canvas.drawRect(36f, y, (PAGE_WIDTH - 36).toFloat(), y + 18f, zebraPaint)
            }
            val medal = when (i) {
                0 -> "🥇 1."
                1 -> "🥈 2."
                2 -> "🥉 3."
                else -> "${i + 1}."
            }
            canvas.drawText(medal, 42f, y + 13f, if (i < 3) medalPaint else tdTextPaint)
            canvas.drawText(st.fullName, 100f, y + 13f, tdTextPaint)
            canvas.drawText("#${st.studentNumber}", 280f, y + 13f, tdTextPaint)
            canvas.drawText("$bookCount Kitap", 370f, y + 13f, tdTextPaint)
            canvas.drawText("$pageCount Sayfa", 480f, y + 13f, tdTextPaint)
            y += 18f
        }

        drawFooter(canvas, 1, 1)
        pdf.finishPage(page)

        FileOutputStream(file).use { pdf.writeTo(it) }
        pdf.close()
        return getFileUri(context, file)
    }

    // ---------------------------------------------------------------------------------------------
    // 8. EN ÇOK SAYFA OKUYAN ÖĞRENCİLER LİSTESİ (PDF)
    // ---------------------------------------------------------------------------------------------
    fun exportTopPageReadersPdf(
        context: Context,
        classRoom: ClassRoom,
        records: List<ReadingRecord>,
        students: List<Student>
    ): Uri {
        val file = createPdfFile(context, "En_Cok_Sayfa_Okuyan_Ogrenciler", classRoom)
        val pdf = PdfDocument()

        val ranking = students.map { st ->
            val stRecords = records.filter { it.studentId == st.id }
            Triple(st, stRecords.sumOf { it.pageCount }, stRecords.size)
        }.sortedByDescending { it.second } // Toplam okunan sayfaya göre azalan

        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = pdf.startPage(pageInfo)
        val canvas = page.canvas

        var y = drawDocumentHeader(canvas, context, classRoom, "En Çok Sayfa Okuyan Öğrenciler Listesi", "Toplam Sayfa Hacmine Göre Sıralama")

        val thBgPaint = Paint().apply { color = navyTheme; style = Paint.Style.FILL }
        val thTextPaint = Paint().apply { color = Color.WHITE; textSize = 9f; typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true }
        val tdTextPaint = Paint().apply { color = darkText; textSize = 8.5f; isAntiAlias = true }
        val zebraPaint = Paint().apply { color = zebraColor; style = Paint.Style.FILL }
        val medalPaint = Paint().apply { color = 0xFFD97706.toInt(); textSize = 9.5f; typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true }

        canvas.drawRect(36f, y, (PAGE_WIDTH - 36).toFloat(), y + 20f, thBgPaint)
        canvas.drawText("Derece", 42f, y + 14f, thTextPaint)
        canvas.drawText("Öğrenci Adı Soyadı", 100f, y + 14f, thTextPaint)
        canvas.drawText("Okul No", 280f, y + 14f, thTextPaint)
        canvas.drawText("Toplam Okunan Sayfa", 370f, y + 14f, thTextPaint)
        canvas.drawText("Okunan Kitap", 490f, y + 14f, thTextPaint)
        y += 20f

        ranking.forEachIndexed { i, (st, pageCount, bookCount) ->
            if (i % 2 == 1) {
                canvas.drawRect(36f, y, (PAGE_WIDTH - 36).toFloat(), y + 18f, zebraPaint)
            }
            val medal = when (i) {
                0 -> "🥇 1."
                1 -> "🥈 2."
                2 -> "🥉 3."
                else -> "${i + 1}."
            }
            canvas.drawText(medal, 42f, y + 13f, if (i < 3) medalPaint else tdTextPaint)
            canvas.drawText(st.fullName, 100f, y + 13f, tdTextPaint)
            canvas.drawText("#${st.studentNumber}", 280f, y + 13f, tdTextPaint)
            canvas.drawText("$pageCount Sayfa", 370f, y + 13f, tdTextPaint)
            canvas.drawText("$bookCount Kitap", 490f, y + 13f, tdTextPaint)
            y += 18f
        }

        drawFooter(canvas, 1, 1)
        pdf.finishPage(page)

        FileOutputStream(file).use { pdf.writeTo(it) }
        pdf.close()
        return getFileUri(context, file)
    }

    // ---------------------------------------------------------------------------------------------
    // 9. GÜNLÜK ORTALAMA OKUNAN SAYFA SAYISI LİSTESİ (PDF)
    // Bir öğrencinin okuduğu kitapların günlük ortalama kaç sayfa olduğuna dair liste (En yüksekten en düşüğe)
    // ---------------------------------------------------------------------------------------------
    fun exportDailyAveragePagesPdf(
        context: Context,
        classRoom: ClassRoom,
        records: List<ReadingRecord>,
        students: List<Student>
    ): Uri {
        val file = createPdfFile(context, "Gunluk_Ortalama_Okunan_Sayfa_Listesi", classRoom)
        val pdf = PdfDocument()

        val ranking = students.map { st ->
            val stRecords = records.filter { it.studentId == st.id }
            val totalPages = stRecords.sumOf { it.pageCount }
            val totalDays = stRecords.sumOf { r ->
                val days = ((r.endDate - r.startDate) / (1000 * 60 * 60 * 24)).toInt()
                days.coerceAtLeast(1)
            }.coerceAtLeast(1)

            val dailyAvg = if (stRecords.isNotEmpty()) totalPages.toDouble() / totalDays.toDouble() else 0.0
            DailyAvgItem(st, dailyAvg, totalPages, totalDays, stRecords.size)
        }.sortedByDescending { it.dailyAverage }

        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = pdf.startPage(pageInfo)
        val canvas = page.canvas

        var y = drawDocumentHeader(canvas, context, classRoom, "Günlük Ortalama Okunan Sayfa Sayısı Listesi", "En Yüksek Ortalamadan En Düşük Ortalamaya Sıralı")

        val thBgPaint = Paint().apply { color = navyTheme; style = Paint.Style.FILL }
        val thTextPaint = Paint().apply { color = Color.WHITE; textSize = 9f; typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true }
        val tdTextPaint = Paint().apply { color = darkText; textSize = 8.5f; isAntiAlias = true }
        val zebraPaint = Paint().apply { color = zebraColor; style = Paint.Style.FILL }
        val avgPaint = Paint().apply { color = tealTheme; textSize = 9.5f; typeface = Typeface.DEFAULT_BOLD; isAntiAlias = true }

        canvas.drawRect(36f, y, (PAGE_WIDTH - 36).toFloat(), y + 20f, thBgPaint)
        canvas.drawText("Sıra", 42f, y + 14f, thTextPaint)
        canvas.drawText("Öğrenci Adı Soyadı", 85f, y + 14f, thTextPaint)
        canvas.drawText("Okul No", 250f, y + 14f, thTextPaint)
        canvas.drawText("Günlük Ortalama", 320f, y + 14f, thTextPaint)
        canvas.drawText("Toplam Sayfa", 430f, y + 14f, thTextPaint)
        canvas.drawText("Okuma Günü", 505f, y + 14f, thTextPaint)
        y += 20f

        ranking.forEachIndexed { i, item ->
            if (i % 2 == 1) {
                canvas.drawRect(36f, y, (PAGE_WIDTH - 36).toFloat(), y + 18f, zebraPaint)
            }
            canvas.drawText("${i + 1}.", 42f, y + 13f, tdTextPaint)
            canvas.drawText(item.student.fullName, 85f, y + 13f, tdTextPaint)
            canvas.drawText("#${item.student.studentNumber}", 250f, y + 13f, tdTextPaint)

            val avgStr = String.format(Locale("tr"), "%.1f sayfa/gün", item.dailyAverage)
            canvas.drawText(avgStr, 320f, y + 13f, avgPaint)

            canvas.drawText("${item.totalPages} s.", 430f, y + 13f, tdTextPaint)
            canvas.drawText("${item.totalDays} gün (${item.bookCount} ktp)", 505f, y + 13f, tdTextPaint)
            y += 18f
        }

        drawFooter(canvas, 1, 1)
        pdf.finishPage(page)

        FileOutputStream(file).use { pdf.writeTo(it) }
        pdf.close()
        return getFileUri(context, file)
    }

    private data class DailyAvgItem(
        val student: Student,
        val dailyAverage: Double,
        val totalPages: Int,
        val totalDays: Int,
        val bookCount: Int
    )
}
