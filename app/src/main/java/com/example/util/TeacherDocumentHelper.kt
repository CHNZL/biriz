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
import com.example.data.model.ClassRoom
import com.example.data.model.Student
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

enum class DilekceType(val title: String, val code: String) {
    MAZERET_IZNI("Mazeret İzni Dilekçesi", "657-DMK"),
    SEVK_RAPOR("Sevk / Sağlık Raporu Bildirimi", "RAPOR-BILD"),
    SINAV_ANALIZ("Sınav & Başarı Analiz Raporu", "ANALIZ-MEB"),
    REHBERLIK_VELI("Öğrenci & Veli Görüşme Formu", "REHBERLIK")
}

data class ZumreToplantiState(
    val academicYear: String = "2026-2027",
    val semester: String = "1. Dönem Başı",
    val subjectName: String = "Sınıf Öğretmenleri Zümresi",
    val meetingDate: String = SimpleDateFormat("dd.MM.yyyy", Locale("tr")).format(Date()),
    val meetingTime: String = "14:30",
    val meetingLocation: String = "Öğretmenler Odası",
    val chairman: String = "",
    val participants: List<String> = listOf("Zümre Başkanı Öğretmen", "Şube Öğretmeni"),
    val agendaItems: List<String> = listOf(
        "Açılış, yoklama ve zümre başkanının seçimi.",
        "Bir önceki döneme ait zümre kararlarının değerlendirilmesi.",
        "MEB Öğretim Programları ve ders kazanımlarının incelenmesi.",
        "Yıllık planların hazırlanması ve ders araç-gereçlerinin planlanması.",
        "Öğrenci başarısını artırıcı tedbirlerin kararlaştırılması.",
        "Ölçme ve değerlendirme kriterleri ve ortak sınav takvimi.",
        "Sosyal etkinlikler, kulüpler ve veli iş birliği ilkeleri.",
        "Dilek ve temenniler, kapanış."
    ),
    val decisions: List<String> = listOf(
        "Ders planları MEB müfredat takvimine uygun şekilde haftalık olarak takip edilecektir.",
        "Derslerde görsel materyaller, teknoloji ve akıllı tahta uygulamaları etkin kullanılacaktır.",
        "Öğrenme güçlüğü çeken öğrencilere Bireyselleştirilmiş Eğitim Planı (BEP) uygulanacaktır.",
        "Sınıf içi kitap okuma saatleri haftada en az 2 ders saati olarak sürdürülecektir.",
        "Ortak değerlendirmeler MEB ölçme kriterlerine göre standartlaştırılacaktır."
    )
)

data class VeliToplantiState(
    val termName: String = "1. Dönem 1. Olağan Sınıf Veli Toplantısı",
    val meetingDate: String = SimpleDateFormat("dd.MM.yyyy", Locale("tr")).format(Date()),
    val meetingTime: String = "15:00",
    val classroomName: String = "",
    val agendaItems: List<String> = listOf(
        "Tanışma ve toplantı açılışı.",
        "Sınıf ve okul kuralları, kılık kıyafet düzeni.",
        "Öğrencilerin ders çalışma alışkanlıkları ve ev ödevleri.",
        "Devam-devamsızlık durumu ve e-Okul bilgilendirmeleri.",
        "Veli-öğretmen-öğrenci iletişimi ve BİRİZ veli takip sistemi.",
        "Dilek, temenniler ve veli imza sirküsü."
    ),
    val decisions: List<String> = listOf(
        "Öğrencilerin okul giriş ve çıkış saatlerine titizlikle uymaları sağlanacaktır.",
        "Beslenme çantalarının okul rehberliğinde sağlıklı gıdalarla hazırlanması tavsiye edilmiştir.",
        "Velilerin öğretmenle görüşmeleri belirlenen haftalık görüşme saatlerinde yapılacaktır.",
        "Ödev takipleri ve sınıf duyuruları BİRİZ platformu üzerinden düzenli izlenecektir.",
        "Öğrenci tablet/ekran sürelerinin evde sınırlandırılması konusunda veliler mutabık kalmıştır."
    )
)

data class SosyalKulupState(
    val clubName: String = "Kütüphanecilik ve Okuma Kulübü",
    val advisorTeacher: String = "",
    val presidentStudentName: String = "",
    val vicePresidentStudentName: String = "",
    val secretaryStudentName: String = "",
    val treasurerStudentName: String = "",
    val communityServiceTopic: String = "Kitap Kardeşliği: Okul Kütüphanesini Zenginleştirme ve Köy Okullarına Kitap Bağışı",
    val monthlyPlan: List<Pair<String, String>> = listOf(
        "Ekim" to "Kulüp tüzüğünün okunması, yönetim kurulu seçimi ve görev dağılımı.",
        "Kasım" to "Atatürk Haftası panosu hazırlanması ve 'Cumhuriyet ve Kitap' temalı okuma saati.",
        "Aralık" to "Kitap ayraçları atölyesi ve sınıf içi en çok kitap okuyan öğrencilerin ödüllendirilmesi.",
        "Ocak" to "Yarıyıl tatili okuma listelerinin belirlenmesi ve kitap tanıtım bülteni.",
        "Şubat" to "Kütüphane düzenleme ve kitap sınıflandırma (Dewey sistemi) eğitimi.",
        "Mart" to "Kütüphaneler Haftası etkinlikleri ve yazar söyleşisi planlaması.",
        "Nisan" to "Dünya Çocuk Kitapları Günü drama etkinliği ve masal anlatım saati.",
        "Mayıs" to "Toplum hizmeti projesi: Toplanan kitapların kardeş okula ulaştırılması.",
        "Haziran" to "Yıl sonu kulüp faaliyet raporunun hazırlanması ve kulüp dosyasının idareye teslimi."
    )
)

data class OkulGeziState(
    val destination: String = "İl Bilim ve Tabiat Tarihi Müzesi",
    val purpose: String = "Öğrencilerin bilime ilgisini artırmak, doğa ve tarih bilincini yerinde gözlemleyerek pekiştirmek.",
    val tripDate: String = SimpleDateFormat("dd.MM.yyyy", Locale("tr")).format(Date()),
    val departureTime: String = "09:00",
    val returnTime: String = "15:30",
    val leaderTeacher: String = "",
    val accompanyingTeachers: List<String> = listOf("Sınıf Öğretmeni", "Rehber Öğretmen"),
    val transportType: String = "MEB Onaylı Okul Servis / Turizm Otobüsü",
    val busPlateAndDriver: String = "06 BİR 2026 / Ahmet Yılmaz (0555 123 4567)",
    val participatingStudentIds: Set<String> = emptySet()
)

data class DilekceState(
    val type: DilekceType = DilekceType.MAZERET_IZNI,
    val subject: String = "657 Sayılı Kanun Madde 104 Uyarınca Mazeret İzni Talebi",
    val dateRange: String = "2 (İki) Gün",
    val dateSpan: String = SimpleDateFormat("dd.MM.yyyy", Locale("tr")).format(Date()),
    val detailText: String = "657 sayılı Devlet Memurları Kanununun ilgili maddesi uyarınca şahsi mazeretime binaen belirtilen tarihlerde izinli sayılmam hususunda gereğini saygılarımla arz ederim."
)

object TeacherDocumentHelper {

    val AVAILABLE_CLUBS = listOf(
        "Kütüphanecilik ve Okuma Kulübü",
        "Akıl Oyunları ve Satranç Kulübü",
        "Yeşilay, Kızılay ve Sağlık Kulübü",
        "Bilim, Fen ve Teknoloji Kulübü",
        "Çevre ve Doğayı Koruma Kulübü",
        "Afete Hazırlık ve Sivil Savunma Kulübü",
        "Kültür, Edebiyat ve Tiyatro Kulübü",
        "Müzik ve Sanat Kulübü",
        "Spor ve İzcilik Kulübü",
        "Sosyal Dayanışma ve Demokrasi Kulübü"
    )

    private fun escapeXml(text: String): String {
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    private fun writeXlsx(file: File, sheetName: String, headers: List<String>, rows: List<List<String>>) {
        ZipOutputStream(FileOutputStream(file)).use { zos ->
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

            zos.putNextEntry(ZipEntry("_rels/.rels"))
            val rels = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>""".trimIndent()
            zos.write(rels.toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            zos.putNextEntry(ZipEntry("xl/workbook.xml"))
            val wb = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
<sheets><sheet name="${escapeXml(sheetName)}" sheetId="1" r:id="rId1"/></sheets>
</workbook>""".trimIndent()
            zos.write(wb.toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            zos.putNextEntry(ZipEntry("xl/_rels/workbook.xml.rels"))
            val wbRels = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
</Relationships>""".trimIndent()
            zos.write(wbRels.toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            zos.putNextEntry(ZipEntry("xl/worksheets/sheet1.xml"))
            val sheetSb = StringBuilder()
            sheetSb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
            sheetSb.append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""")
            sheetSb.append("<sheetData>")

            // Header row
            sheetSb.append("<row r=\"1\">")
            for ((cIdx, h) in headers.withIndex()) {
                val colLetter = ('A'.code + cIdx).toChar().toString()
                sheetSb.append("<c r=\"${colLetter}1\" t=\"inlineStr\"><is><t>${escapeXml(h)}</t></is></c>")
            }
            sheetSb.append("</row>")

            // Data rows
            for ((rIdx, row) in rows.withIndex()) {
                val rowNum = rIdx + 2
                sheetSb.append("<row r=\"$rowNum\">")
                for ((cIdx, cellVal) in row.withIndex()) {
                    val colLetter = ('A'.code + cIdx).toChar().toString()
                    sheetSb.append("<c r=\"$colLetter$rowNum\" t=\"inlineStr\"><is><t>${escapeXml(cellVal)}</t></is></c>")
                }
                sheetSb.append("</row>")
            }

            sheetSb.append("</sheetData></worksheet>")
            zos.write(sheetSb.toString().toByteArray(Charsets.UTF_8))
            zos.closeEntry()
        }
    }

    // --- 1. ZÜMRE TOPLANTISI ---
    fun generateZumreShareText(classRoom: ClassRoom, state: ZumreToplantiState): String {
        val sb = StringBuilder()
        sb.append("📋 T.C. MİLLÎ EĞİTİM BAKANLIĞI\n")
        sb.append("🏛️ ${classRoom.schoolName}\n")
        sb.append("📌 ${state.academicYear} EĞİTİM-ÖĞRETİM YILI ${state.semester.uppercase(Locale("tr"))}\n")
        sb.append("👥 ${state.subjectName.uppercase(Locale("tr"))} TOPLANTI TUTANAĞI\n\n")

        sb.append("📅 Tarih & Saat: ${state.meetingDate} - ${state.meetingTime}\n")
        sb.append("📍 Toplantı Yeri: ${state.meetingLocation}\n")
        sb.append("👤 Zümre Başkanı: ${state.chairman.ifBlank { classRoom.teacherName }}\n")
        if (state.participants.isNotEmpty()) {
            sb.append("👥 Katılımcılar: ${state.participants.joinToString(", ")}\n")
        }
        sb.append("\n━━━━━━━━━━━━━━━━━━━━━━\n")
        sb.append("📝 GÜNDEM MADDELERİ:\n")
        state.agendaItems.forEachIndexed { i, item ->
            sb.append("${i + 1}. $item\n")
        }
        sb.append("\n━━━━━━━━━━━━━━━━━━━━━━\n")
        sb.append("✅ ALINAN KARARLAR:\n")
        state.decisions.forEachIndexed { i, dec ->
            sb.append("${i + 1}. $dec\n")
        }
        sb.append("\n✍️ Zümre Kurulu Üyeleri ve İmzaları\n")
        sb.append("BİRİZ Öğretmen Odası Evrak Sistemi ✨")
        return sb.toString()
    }

    fun exportZumreExcel(context: Context, classRoom: ClassRoom, state: ZumreToplantiState): Uri {
        val fileName = "${classRoom.schoolName.take(15).replace(" ", "_")}_Zumre_Tutanagi.xlsx"
        val file = File(context.cacheDir, fileName)
        val headers = listOf("Madde No", "Gündem Konusu", "Alınan Karar", "Sorumlu / Açıklama")
        val maxItems = maxOf(state.agendaItems.size, state.decisions.size)
        val rows = (0 until maxItems).map { i ->
            val agenda = state.agendaItems.getOrElse(i) { "-" }
            val decision = state.decisions.getOrElse(i) { "-" }
            listOf("${i + 1}", agenda, decision, state.chairman.ifBlank { classRoom.teacherName })
        }
        writeXlsx(file, "Zümre Tutanağı", headers, rows)
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    // --- 2. VELİ TOPLANTISI ---
    fun generateVeliToplantiShareText(classRoom: ClassRoom, state: VeliToplantiState, students: List<Student>): String {
        val sb = StringBuilder()
        sb.append("👨‍👩‍👧‍👦 T.C. MİLLÎ EĞİTİM BAKANLIĞI\n")
        sb.append("🏛️ ${classRoom.schoolName}\n")
        sb.append("🏫 ${classRoom.gradeLevel}/${classRoom.section} SINIFI ${state.termName.uppercase(Locale("tr"))}\n\n")

        sb.append("📅 Tarih: ${state.meetingDate} | Saat: ${state.meetingTime}\n")
        sb.append("📍 Derslik: ${state.classroomName.ifBlank { "${classRoom.gradeLevel}/${classRoom.section} Dersliği" }}\n")
        sb.append("👩‍🏫 Sınıf Öğretmeni: ${classRoom.teacherName}\n")
        sb.append("📊 Kayıtlı Öğrenci Sayısı: ${students.size}\n\n")

        sb.append("━━━━━━━━━━━━━━━━━━━━━━\n")
        sb.append("📝 TOPLANTI GÜNDEMİ:\n")
        state.agendaItems.forEachIndexed { i, item ->
            sb.append("${i + 1}. $item\n")
        }
        sb.append("\n━━━━━━━━━━━━━━━━━━━━━━\n")
        sb.append("📌 ALINAN KARARLAR:\n")
        state.decisions.forEachIndexed { i, dec ->
            sb.append("${i + 1}. $dec\n")
        }
        sb.append("\nVelilerimizin katılım ve destekleri için teşekkür ederiz.\n")
        sb.append("BİRİZ Veli Bilgilendirme Sistemi ✨")
        return sb.toString()
    }

    fun exportVeliToplantiExcel(context: Context, classRoom: ClassRoom, state: VeliToplantiState, students: List<Student>): Uri {
        val fileName = "${classRoom.schoolName.take(15).replace(" ", "_")}_Veli_Toplanti_Imza_Sirkusu.xlsx"
        val file = File(context.cacheDir, fileName)
        val headers = listOf("Sıra No", "Okul No", "Öğrenci Adı Soyadı", "Katılan Veli Adı Soyadı", "İmza")
        val sortedStudents = students.sortedBy { it.studentNumber.toIntOrNull() ?: 9999 }
        val rows = sortedStudents.mapIndexed { i, s ->
            listOf(
                "${i + 1}",
                s.studentNumber,
                s.fullName,
                "...................................",
                "[  İmza  ]"
            )
        }
        writeXlsx(file, "Veli İmza Sirküsü", headers, rows)
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    // --- 3. SOSYAL KULÜP ---
    fun generateSosyalKulupShareText(classRoom: ClassRoom, state: SosyalKulupState): String {
        val sb = StringBuilder()
        sb.append("🤝 T.C. MİLLÎ EĞİTİM BAKANLIĞI\n")
        sb.append("🏛️ ${classRoom.schoolName}\n")
        sb.append("🌟 ${state.clubName.uppercase(Locale("tr"))}\n")
        sb.append("YILLIK ÇALIŞMA PLANI VE YÖNETİM KURULU\n\n")

        sb.append("👩‍🏫 Danışman Öğretmen: ${state.advisorTeacher.ifBlank { classRoom.teacherName }}\n\n")
        sb.append("👑 KULÜP YÖNETİM KURULU:\n")
        sb.append("• Kulüp Başkanı: ${state.presidentStudentName.ifBlank { "Belirlenecek" }}\n")
        sb.append("• Başkan Yardımcısı: ${state.vicePresidentStudentName.ifBlank { "Belirlenecek" }}\n")
        sb.append("• Yazman: ${state.secretaryStudentName.ifBlank { "Belirlenecek" }}\n")
        sb.append("• Sayman / Üye: ${state.treasurerStudentName.ifBlank { "Belirlenecek" }}\n\n")

        sb.append("🌱 Toplum Hizmeti: ${state.communityServiceTopic}\n\n")
        sb.append("📅 YILLIK ÇALIŞMA TAKVİMİ:\n")
        state.monthlyPlan.forEach { (month, activity) ->
            sb.append("• $month: $activity\n")
        }
        sb.append("\nBİRİZ Öğretmen Odası ✨")
        return sb.toString()
    }

    fun exportSosyalKulupExcel(context: Context, classRoom: ClassRoom, state: SosyalKulupState): Uri {
        val fileName = "${classRoom.schoolName.take(15).replace(" ", "_")}_Sosyal_Kulup_Plani.xlsx"
        val file = File(context.cacheDir, fileName)
        val headers = listOf("Sıra No", "Ay", "Faaliyet / Etkinlik Konusu", "Sorumlu")
        val rows = state.monthlyPlan.mapIndexed { i, (month, act) ->
            listOf("${i + 1}", month, act, state.advisorTeacher.ifBlank { classRoom.teacherName })
        }
        writeXlsx(file, "Kulüp Çalışma Planı", headers, rows)
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    // --- 4. OKUL GEZİSİ ---
    fun generateOkulGeziShareText(classRoom: ClassRoom, state: OkulGeziState, selectedStudentsCount: Int): String {
        val sb = StringBuilder()
        sb.append("🚌 MEB SOSYAL ETKİNLİKLER OKUL GEZİSİ\n")
        sb.append("🏛️ ${classRoom.schoolName} - ${classRoom.gradeLevel}/${classRoom.section} Sınıfı\n")
        sb.append("📍 Gezi Yeri: ${state.destination}\n\n")

        sb.append("📅 Gezi Tarihi: ${state.tripDate}\n")
        sb.append("⏰ Hareket: ${state.departureTime} | Dönüş: ${state.returnTime}\n")
        sb.append("🎯 Gezinin Amacı: ${state.purpose}\n\n")

        sb.append("👩‍🏫 Kafile Başkanı: ${state.leaderTeacher.ifBlank { classRoom.teacherName }}\n")
        sb.append("🚌 Ulaşım / Araç: ${state.transportType} (${state.busPlateAndDriver})\n")
        sb.append("👥 Katılacak Öğrenci Sayısı: $selectedStudentsCount Öğrenci\n\n")

        sb.append("📢 Sayın Velimiz;\n")
        sb.append("Öğrencimizin yukarıda ayrıntıları belirtilen okul gezisine katılmasına izin veriyorsanız veli muvafakat belgesini imzalayarak sınıf öğretmenine teslim etmenizi rica ederiz.\n\n")
        sb.append("BİRİZ Okul Gezi Koordinasyonu ✨")
        return sb.toString()
    }

    fun exportOkulGeziExcel(context: Context, classRoom: ClassRoom, state: OkulGeziState, participatingStudents: List<Student>): Uri {
        val fileName = "${classRoom.schoolName.take(15).replace(" ", "_")}_Gezi_Ogrenci_Listesi.xlsx"
        val file = File(context.cacheDir, fileName)
        val headers = listOf("Sıra", "Okul No", "Öğrenci Adı Soyadı", "Cinsiyet", "Veli İzin Durumu", "Acil Durum İletişim")
        val sorted = participatingStudents.sortedBy { it.studentNumber.toIntOrNull() ?: 9999 }
        val rows = sorted.mapIndexed { i, s ->
            listOf(
                "${i + 1}",
                s.studentNumber,
                s.fullName,
                s.gender,
                "İzin Verildi (Muvafakat Alındı)",
                "............................"
            )
        }
        writeXlsx(file, "Gezi Katılımcı Listesi", headers, rows)
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    // --- 5. RESMİ DİLEKÇE METNİ ---
    fun generateDilekceShareText(classRoom: ClassRoom, state: DilekceState): String {
        val sb = StringBuilder()
        sb.append("T.C.\n")
        sb.append("${classRoom.cityName.uppercase(Locale("tr"))} VALİLİĞİ / KAYMAKAMLIĞI\n")
        sb.append("${classRoom.districtName.uppercase(Locale("tr"))} İLÇE MİLLÎ EĞİTİM MÜDÜRLÜĞÜ\n")
        sb.append("${classRoom.schoolName.uppercase(Locale("tr"))} MÜDÜRLÜĞÜNE\n\n")

        sb.append("KONU: ${state.subject}\n")
        sb.append("TARİH: ${state.dateSpan}\n\n")

        sb.append("Okulunuzda ${classRoom.gradeLevel}/${classRoom.section} Sınıf Öğretmeni olarak görev yapmaktayım.\n\n")
        sb.append("${state.detailText}\n\n")
        sb.append("Gereğini bilgilerinize saygılarımla arz ederim.\n\n")

        sb.append("ADRES / İLETİŞİM:\n")
        sb.append("${classRoom.teacherName}\n")
        sb.append("Sınıf Öğretmeni\n")
        sb.append("Tarih: ${state.dateSpan}\n")
        sb.append("İmza: ............................\n")
        return sb.toString()
    }

    // --- GENEL RESMİ A4 PDF ÇIKTISI (MEB Standart Antet ve İmzalarla) ---
    fun exportOfficialDocumentPdf(
        context: Context,
        classRoom: ClassRoom,
        documentTitle: String,
        subTitle: String,
        infoRows: List<Pair<String, String>>,
        numberedSections: List<Pair<String, List<String>>>,
        signers: List<String>
    ): Uri {
        val pdfDocument = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // 1. Üst Beyaz Zemin
        paint.color = Color.WHITE
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat(), paint)

        // BİRİZ Logo
        val emblemBitmap = runCatching {
            val drawable = ContextCompat.getDrawable(context, R.drawable.ic_biriz_emblem) ?: return@runCatching null
            val bitmap = Bitmap.createBitmap(80, 80, Bitmap.Config.ARGB_8888)
            val c = Canvas(bitmap)
            drawable.setBounds(0, 0, 80, 80)
            drawable.draw(c)
            bitmap
        }.getOrNull()

        if (emblemBitmap != null) {
            canvas.drawBitmap(emblemBitmap, null, RectF(36f, 20f, 36f + 40f, 20f + 40f), paint)
        }

        // T.C. Millî Eğitim Bakanlığı Başlığı
        paint.color = Color.parseColor("#13294B")
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 11f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("T.C.", pageWidth / 2f, 30f, paint)
        canvas.drawText("${classRoom.cityName.uppercase(Locale("tr"))} VALİLİĞİ / İLÇE MİLLÎ EĞİTİM MÜDÜRLÜĞÜ", pageWidth / 2f, 44f, paint)
        paint.textSize = 10f
        canvas.drawText(classRoom.schoolName.uppercase(Locale("tr")), pageWidth / 2f, 57f, paint)

        // Üst İnce Ayırıcı Çizgi
        paint.color = Color.parseColor("#28B8C2")
        paint.strokeWidth = 2f
        canvas.drawLine(36f, 68f, pageWidth - 36f, 68f, paint)

        // Doküman Başlığı
        paint.color = Color.parseColor("#0F172A")
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(documentTitle.uppercase(Locale("tr")), pageWidth / 2f, 88f, paint)

        paint.textSize = 9.5f
        paint.color = Color.parseColor("#475569")
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText(subTitle, pageWidth / 2f, 101f, paint)

        // Bilgi Tablosu (2 Sütunlu Zarif Kart)
        var curY = 118f
        paint.textAlign = Paint.Align.LEFT
        val boxRect = RectF(36f, curY, pageWidth - 36f, curY + (infoRows.size * 16f) + 10f)
        paint.color = Color.parseColor("#F8FAFC")
        canvas.drawRoundRect(boxRect, 6f, 6f, paint)
        paint.color = Color.parseColor("#CBD5E1")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawRoundRect(boxRect, 6f, 6f, paint)
        paint.style = Paint.Style.FILL

        curY += 15f
        infoRows.forEach { (lbl, valStr) ->
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.color = Color.parseColor("#1E293B")
            paint.textSize = 8.5f
            canvas.drawText("$lbl: ", 46f, curY, paint)
            val lblWidth = paint.measureText("$lbl: ")
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.color = Color.parseColor("#334155")
            canvas.drawText(valStr, 46f + lblWidth, curY, paint)
            curY += 16f
        }

        curY += 12f

        // Numaralı Bölümler (Gündem, Kararlar vb.)
        numberedSections.forEach { (secTitle, items) ->
            paint.color = Color.parseColor("#13294B")
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 9.5f
            canvas.drawText(secTitle.uppercase(Locale("tr")), 36f, curY, paint)

            paint.color = Color.parseColor("#94A3B8")
            paint.strokeWidth = 1f
            canvas.drawLine(36f, curY + 3f, pageWidth - 36f, curY + 3f, paint)
            curY += 14f

            items.forEachIndexed { idx, item ->
                paint.color = Color.parseColor("#1E293B")
                paint.textSize = 8.5f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

                val prefix = "${idx + 1}. "
                val fullText = prefix + item
                // Çok uzun metinleri saralım
                if (fullText.length > 95) {
                    val line1 = fullText.take(90)
                    val line2 = "    " + fullText.substring(90)
                    canvas.drawText(line1, 40f, curY, paint)
                    curY += 12f
                    canvas.drawText(line2, 40f, curY, paint)
                } else {
                    canvas.drawText(fullText, 40f, curY, paint)
                }
                curY += 13f
            }
            curY += 8f
        }

        // İmzalar Kutusu (Alt Kısım)
        val signY = (pageHeight - 90f).coerceAtLeast(curY + 20f)
        paint.color = Color.parseColor("#0F172A")
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 9f
        paint.textAlign = Paint.Align.CENTER

        val signerCount = signers.size.coerceAtLeast(1)
        val colWidth = (pageWidth - 72f) / signerCount
        signers.forEachIndexed { i, signerName ->
            val centerX = 36f + (i * colWidth) + (colWidth / 2f)
            canvas.drawText(signerName, centerX, signY, paint)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textSize = 8f
            paint.color = Color.parseColor("#475569")
            canvas.drawText("İmza: ........................", centerX, signY + 16f, paint)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = 9f
            paint.color = Color.parseColor("#0F172A")
        }

        // Alt Bilgi Notu
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 7.5f
        paint.color = Color.parseColor("#94A3B8")
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("BİRİZ İlkokul & Ortaokul Yönetim Sistemi • Resmi MEB Belge Çıktısı", pageWidth / 2f, pageHeight - 20f, paint)

        pdfDocument.finishPage(page)

        val safeTitle = documentTitle.replace(" ", "_").filter { it.isLetterOrDigit() || it == '_' }.take(20)
        val file = File(context.cacheDir, "${safeTitle}_Evrak.pdf")
        pdfDocument.writeTo(FileOutputStream(file))
        pdfDocument.close()

        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }
}
