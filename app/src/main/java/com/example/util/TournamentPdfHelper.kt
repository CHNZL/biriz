package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.R
import com.example.data.model.Student
import com.example.data.model.Tournament
import com.example.data.model.TournamentMatch
import com.example.data.model.TournamentStanding
import com.example.data.model.TournamentStatus
import com.example.data.model.TournamentType
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Turnuva türüne (Eleme, Lig, Grup, Grup+Eleme) özel, indirildiği andaki güncel skorları,
 * puan durumlarını, grup son maç tablolarını ve görsel eleme ağacı şemasını üreten PDF motoru.
 */
object TournamentPdfHelper {

    private const val PORTRAIT_W = 595
    private const val PORTRAIT_H = 842
    private const val LANDSCAPE_W = 842
    private const val LANDSCAPE_H = 595

    private const val COLOR_NAVY = 0xFF1B365D.toInt()
    private const val COLOR_TURQUOISE = 0xFF0EA5E9.toInt()
    private const val COLOR_GOLD = 0xFFF59E0B.toInt()
    private const val COLOR_GOLD_DARK = 0xFFB45309.toInt()
    private const val COLOR_GOLD_BG = 0xFFFEF3C7.toInt()
    private const val COLOR_GREEN = 0xFF10B981.toInt()
    private const val COLOR_GREEN_DARK = 0xFF065F46.toInt()
    private const val COLOR_GREEN_BG = 0xFFDCFCE7.toInt()
    private const val COLOR_RED = 0xFFEF4444.toInt()
    private const val COLOR_RED_BG = 0xFFFEE2E2.toInt()
    private const val COLOR_TEXT = 0xFF1E293B.toInt()
    private const val COLOR_MUTED = 0xFF64748B.toInt()
    private const val COLOR_ZEBRA = 0xFFF8FAFC.toInt()
    private const val COLOR_BORDER = 0xFFCBD5E1.toInt()
    private const val COLOR_SILVER = 0xFF475569.toInt()

    private class PdfCursor(
        val pdf: PdfDocument,
        val tournament: Tournament,
        val dateStr: String,
        val width: Int = PORTRAIT_W,
        val height: Int = PORTRAIT_H
    ) {
        var pageNumber: Int = 0
        lateinit var page: PdfDocument.Page
        lateinit var canvas: Canvas
        var y: Float = 36f

        fun startPage(continuationTitle: String? = null, context: Context? = null, drawFullHeader: Boolean = false) {
            if (pageNumber > 0) {
                drawFooter(canvas, tournament.name, pageNumber, width, height)
                pdf.finishPage(page)
            }
            pageNumber++
            val info = PdfDocument.PageInfo.Builder(width, height, pageNumber).create()
            page = pdf.startPage(info)
            canvas = page.canvas
            if (drawFullHeader && context != null) {
                y = drawDocumentHeader(canvas, context, tournament, dateStr, width)
            } else {
                y = drawContinuationHeader(canvas, tournament, continuationTitle ?: tournament.name, width)
            }
        }

        fun ensureSpace(neededHeight: Float, continuationTitle: String) {
            if (y + neededHeight > height - 36f) {
                startPage(continuationTitle = continuationTitle)
            }
        }

        fun finishCurrentPage() {
            if (pageNumber > 0) {
                drawFooter(canvas, tournament.name, pageNumber, width, height)
                pdf.finishPage(page)
                pageNumber = 0
            }
        }
    }

    fun exportTournamentPdf(
        context: Context,
        tournament: Tournament,
        matches: List<TournamentMatch>,
        standings: List<TournamentStanding>,
        students: List<Student> = emptyList()
    ): Uri {
        val file = File(context.cacheDir, "turnuva_${tournament.id}_${System.currentTimeMillis()}.pdf")
        val pdfDocument = PdfDocument()
        val dateStr = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("tr")).format(Date())

        val resolvedParticipants = TournamentOutcomeHelper.resolveParticipantPairs(
            participantIds = tournament.participants,
            matches = matches,
            students = students
        )
        val computedStandings = if (standings.any { it.playerName == it.playerId && !it.playerId.startsWith("extra:") } || standings.isEmpty()) {
            TournamentEngine.calculateStandings(
                participants = resolvedParticipants,
                matches = matches,
                pointsWin = tournament.pointsWin,
                pointsDraw = tournament.pointsDraw,
                pointsLoss = tournament.pointsLoss
            )
        } else {
            standings
        }
        val podium = TournamentEngine.determinePodium(tournament, matches, computedStandings)
        val resolvedTournament = resolveTournamentNames(tournament, podium, matches, students)

        var nextPageNumber = 1
        when (resolvedTournament.type) {
            TournamentType.ELEME -> {
                val koMatches = matches.filter { it.stage != "group" }
                nextPageNumber = drawBracketPages(
                    pdfDocument = pdfDocument,
                    context = context,
                    tournament = resolvedTournament,
                    knockoutMatches = koMatches,
                    allMatches = matches,
                    students = students,
                    dateStr = dateStr,
                    startPageNum = nextPageNumber,
                    subtitleNote = null
                )
                drawKnockoutFixtureTablePages(
                    pdfDocument = pdfDocument,
                    context = context,
                    tournament = resolvedTournament,
                    knockoutMatches = koMatches,
                    allMatches = matches,
                    students = students,
                    dateStr = dateStr,
                    startPageNum = nextPageNumber,
                    includeFullHeader = koMatches.isEmpty()
                )
            }

            TournamentType.LIG -> {
                drawLeagueReportPages(
                    pdfDocument = pdfDocument,
                    context = context,
                    tournament = resolvedTournament,
                    matches = matches,
                    standings = computedStandings,
                    students = students,
                    dateStr = dateStr
                )
            }

            TournamentType.GRUP -> {
                drawGroupReportPages(
                    pdfDocument = pdfDocument,
                    context = context,
                    tournament = resolvedTournament,
                    matches = matches,
                    students = students,
                    dateStr = dateStr,
                    isGroupElim = false
                )
            }

            TournamentType.GRUP_ELEME -> {
                val groupReports = TournamentOutcomeHelper.buildGroupReports(resolvedTournament, matches, students)
                nextPageNumber = drawGroupReportPages(
                    pdfDocument = pdfDocument,
                    context = context,
                    tournament = resolvedTournament,
                    matches = matches,
                    students = students,
                    dateStr = dateStr,
                    isGroupElim = true
                )

                val existingKnockout = matches.filter { it.stage == "knockout" }
                val isProjected = existingKnockout.isEmpty()
                val bracketMatches = TournamentOutcomeHelper.buildProjectedPlayoffMatches(
                    tournament = resolvedTournament,
                    groupReports = groupReports,
                    existingKnockouts = existingKnockout
                )
                if (bracketMatches.isNotEmpty()) {
                    nextPageNumber = drawBracketPages(
                        pdfDocument = pdfDocument,
                        context = context,
                        tournament = resolvedTournament,
                        knockoutMatches = bracketMatches,
                        allMatches = if (isProjected) matches + bracketMatches else matches,
                        students = students,
                        dateStr = dateStr,
                        startPageNum = nextPageNumber,
                        subtitleNote = if (isProjected) {
                            "Planlanan Play-Off (Eleme) Aşaması Şeması — Grup maçları tamamlandığında kesin eşleşmeler oluşacaktır"
                        } else {
                            "Play-Off (Eleme) Aşaması Güncel Şeması"
                        }
                    )
                }
                if (existingKnockout.isNotEmpty()) {
                    drawKnockoutFixtureTablePages(
                        pdfDocument = pdfDocument,
                        context = context,
                        tournament = resolvedTournament,
                        knockoutMatches = existingKnockout,
                        allMatches = matches,
                        students = students,
                        dateStr = dateStr,
                        startPageNum = nextPageNumber,
                        includeFullHeader = false
                    )
                }
            }
        }

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

    private fun resolveTournamentNames(
        tournament: Tournament,
        podium: Triple<Pair<String, String>?, Pair<String, String>?, Pair<String, String>?>,
        matches: List<TournamentMatch>,
        students: List<Student>
    ): Tournament {
        val wId = tournament.winnerId ?: podium.first?.first
        val rId = tournament.runnerUpId ?: podium.second?.first
        val tId = tournament.thirdPlaceId ?: podium.third?.first
        val wName = wId?.let { TournamentOutcomeHelper.resolveParticipantName(it, matches, students) }
            ?: tournament.winnerName
            ?: podium.first?.second
        val rName = rId?.let { TournamentOutcomeHelper.resolveParticipantName(it, matches, students) }
            ?: tournament.runnerUpName
            ?: podium.second?.second
        val tName = tId?.let { TournamentOutcomeHelper.resolveParticipantName(it, matches, students) }
            ?: tournament.thirdPlaceName
            ?: podium.third?.second

        return tournament.copy(
            winnerId = wId,
            winnerName = wName,
            runnerUpId = rId,
            runnerUpName = rName,
            thirdPlaceId = tId,
            thirdPlaceName = tName
        )
    }

    // -----------------------------------------------------------------
    // 1. GÖRSEL ELEME ŞEMASI (A4 LANDSCAPE BRACKET)
    // -----------------------------------------------------------------
    private fun drawBracketPages(
        pdfDocument: PdfDocument,
        context: Context,
        tournament: Tournament,
        knockoutMatches: List<TournamentMatch>,
        allMatches: List<TournamentMatch>,
        students: List<Student>,
        dateStr: String,
        startPageNum: Int,
        subtitleNote: String?
    ): Int {
        if (knockoutMatches.isEmpty()) return startPageNum

        val columnsMap = TournamentOutcomeHelper.buildBracketColumns(tournament, knockoutMatches, students)
        val roundNumbers = columnsMap.keys.sorted()
        if (roundNumbers.isEmpty()) return startPageNum

        val hasPreElim = TournamentOutcomeHelper.hasPreEliminationRound(knockoutMatches)
        val maxRound = roundNumbers.maxOrNull() ?: 1
        val firstRoundNodes = columnsMap[roundNumbers.first()].orEmpty().filter { !it.isThirdPlaceMatch }
        val r1TooDense = roundNumbers.size >= 2 && firstRoundNodes.size > 10

        var currentPageNum = startPageNum

        // Eğer 1. Turda 10'dan fazla maç varsa (ör. 24-32+ katılımcı), 1. Turu temiz bir 2 sütunlu ön şema sayfasına al,
        // ardından 2. Tur -> Final ağacını ana şema sayfasında sıfır sıkışma ile çiz.
        val treeRoundNumbers = if (r1TooDense) {
            currentPageNum = drawOverflowFirstRoundPage(
                pdfDocument = pdfDocument,
                context = context,
                tournament = tournament,
                roundNum = roundNumbers.first(),
                maxRound = maxRound,
                hasPreElim = hasPreElim,
                nodes = firstRoundNodes,
                dateStr = dateStr,
                pageNum = currentPageNum,
                totalMatches = allMatches.size,
                completedMatches = allMatches.count { it.status == "completed" }
            )
            roundNumbers.drop(1)
        } else {
            roundNumbers
        }

        val pageInfo = PdfDocument.PageInfo.Builder(LANDSCAPE_W, LANDSCAPE_H, currentPageNum).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        var y = drawDocumentHeader(
            canvas = canvas,
            context = context,
            tournament = tournament,
            dateStr = dateStr,
            pageWidth = LANDSCAPE_W,
            matches = allMatches
        )

        if (!subtitleNote.isNullOrBlank()) {
            val noteBg = Paint().apply { color = 0xFFE0F2FE.toInt(); style = Paint.Style.FILL }
            val noteBorder = Paint().apply { color = COLOR_TURQUOISE; style = Paint.Style.STROKE; strokeWidth = 0.8f }
            val noteRect = RectF(28f, y, LANDSCAPE_W - 28f, y + 18f)
            canvas.drawRoundRect(noteRect, 4f, 4f, noteBg)
            canvas.drawRoundRect(noteRect, 4f, 4f, noteBorder)
            val noteText = Paint().apply {
                color = COLOR_NAVY
                textSize = 8.2f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            canvas.drawText("ℹ️ $subtitleNote", 36f, y + 12f, noteText)
            y += 24f
        } else {
            val sectionP = Paint().apply {
                color = COLOR_NAVY
                textSize = 10.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            canvas.drawText("🏆 ELEME AĞACI VE GÜNCEL TUR ŞEMASI", 28f, y + 10f, sectionP)
            y += 18f
        }

        val marginX = 24f
        val colCount = treeRoundNumbers.size
        val gapX = if (colCount <= 1) 0f else (22f).coerceAtMost((LANDSCAPE_W - marginX * 2f) / (colCount * 4.5f))
        val colWidth = ((LANDSCAPE_W - marginX * 2f) - gapX * (colCount - 1)) / colCount

        // Sütun başlıkları
        val headerY = y
        val colHeaderH = 18f
        val colHeaderPaint = Paint().apply { color = COLOR_NAVY; style = Paint.Style.FILL }
        val finalHeaderPaint = Paint().apply { color = COLOR_GOLD_DARK; style = Paint.Style.FILL }
        val colHeaderText = Paint().apply {
            color = Color.WHITE
            textSize = 8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        treeRoundNumbers.forEachIndexed { cIdx, rNum ->
            val colX = marginX + cIdx * (colWidth + gapX)
            val isFinalCol = (rNum == maxRound)
            val rTitle = TournamentOutcomeHelper.getRoundDisplayName(
                round = rNum,
                maxRound = maxRound,
                isKnockout = true,
                hasPreElimination = hasPreElim
            ).uppercase(Locale("tr"))
            val headerRect = RectF(colX, headerY, colX + colWidth, headerY + colHeaderH)
            canvas.drawRoundRect(headerRect, 4f, 4f, if (isFinalCol) finalHeaderPaint else colHeaderPaint)
            val measured = colHeaderText.measureText(rTitle)
            canvas.drawText(rTitle, colX + ((colWidth - measured) / 2f).coerceAtLeast(4f), headerY + 12f, colHeaderText)
        }

        val bracketTop = headerY + colHeaderH + 8f
        val bracketBottom = LANDSCAPE_H - 28f
        val bracketH = bracketBottom - bracketTop

        val maxMainInCol = treeRoundNumbers.maxOf { rNum ->
            columnsMap[rNum].orEmpty().count { !it.isThirdPlaceMatch }
        }.coerceAtLeast(1)

        val slotH = bracketH / maxMainInCol
        val boxHeight = (slotH - 6f).coerceIn(34f, 56f)

        // Düğüm Y merkezlerini hesapla
        val nodeBoxes = mutableMapOf<String, RectF>() // baseCode -> RectF
        val headerStripH = 11f
        val playerRowH = (boxHeight - headerStripH) / 2f

        // En yoğun ilk veya ikinci sütunu baz alarak hizala
        val anchorIdx = if (treeRoundNumbers.size >= 2) {
            val c0 = columnsMap[treeRoundNumbers[0]].orEmpty().count { !it.isThirdPlaceMatch }
            val c1 = columnsMap[treeRoundNumbers[1]].orEmpty().count { !it.isThirdPlaceMatch }
            if (hasPreElim && c1 > 0 && c0 != c1 * 2) 1 else 0
        } else 0

        if (anchorIdx == 1) {
            // 1) Önce Round 2 (ana 2^k ağacı) kutularını yerleştir
            val r2Nodes = columnsMap[treeRoundNumbers[1]].orEmpty().filter { !it.isThirdPlaceMatch }
            val colX1 = marginX + 1 * (colWidth + gapX)
            val sH1 = bracketH / r2Nodes.size.coerceAtLeast(1)
            r2Nodes.forEachIndexed { idx, node ->
                val centerY = bracketTop + (idx + 0.5f) * sH1
                val top = (centerY - boxHeight / 2f).coerceIn(bracketTop, bracketBottom - boxHeight)
                nodeBoxes[node.baseCode] = RectF(colX1, top, colX1 + colWidth, top + boxHeight)
            }

            // 2) Ön Eleme (Round 1) kutularını yerleştir
            val r1Nodes = columnsMap[treeRoundNumbers[0]].orEmpty().filter { !it.isThirdPlaceMatch }
            val colX0 = marginX
            val sH0 = bracketH / r1Nodes.size.coerceAtLeast(1)
            r1Nodes.forEachIndexed { idx, node ->
                val centerY = bracketTop + (idx + 0.5f) * sH0
                val top = (centerY - boxHeight / 2f).coerceIn(bracketTop, bracketBottom - boxHeight)
                nodeBoxes[node.baseCode] = RectF(colX0, top, colX0 + colWidth, top + boxHeight)
            }
        } else {
            val firstNodes = columnsMap[treeRoundNumbers[0]].orEmpty().filter { !it.isThirdPlaceMatch }
            val colX0 = marginX
            val sH0 = bracketH / firstNodes.size.coerceAtLeast(1)
            firstNodes.forEachIndexed { idx, node ->
                val centerY = bracketTop + (idx + 0.5f) * sH0
                val top = (centerY - boxHeight / 2f).coerceIn(bracketTop, bracketBottom - boxHeight)
                nodeBoxes[node.baseCode] = RectF(colX0, top, colX0 + colWidth, top + boxHeight)
            }
        }

        // Sonraki tüm sütunları önceki sütundan orta noktaya yerleştir
        val startLaterCol = if (anchorIdx == 1) 2 else 1
        for (cIdx in startLaterCol until treeRoundNumbers.size) {
            val rNum = treeRoundNumbers[cIdx]
            val colX = marginX + cIdx * (colWidth + gapX)
            val mainNodes = columnsMap[rNum].orEmpty().filter { !it.isThirdPlaceMatch }
            val sH = bracketH / mainNodes.size.coerceAtLeast(1)
            val desiredCenters = mainNodes.mapIndexed { idx, node ->
                val src1 = node.p1SourceBaseCode?.let { nodeBoxes[it]?.centerY() }
                val src2 = node.p2SourceBaseCode?.let { nodeBoxes[it]?.centerY() }
                when {
                    src1 != null && src2 != null -> (src1 + src2) / 2f
                    src1 != null -> src1
                    src2 != null -> src2
                    else -> bracketTop + (idx + 0.5f) * sH
                }
            }.toMutableList()

            // Çakışma engelleme
            val minStep = boxHeight + 4f
            for (k in 1 until desiredCenters.size) {
                if (desiredCenters[k] - desiredCenters[k - 1] < minStep) {
                    desiredCenters[k] = desiredCenters[k - 1] + minStep
                }
            }

            mainNodes.forEachIndexed { idx, node ->
                val top = (desiredCenters[idx] - boxHeight / 2f).coerceIn(bracketTop, bracketBottom - boxHeight)
                nodeBoxes[node.baseCode] = RectF(colX, top, colX + colWidth, top + boxHeight)
            }
        }

        // 3.lük maçını final sütununda alt kısma yerleştir
        val finalRoundNum = treeRoundNumbers.last()
        val thirdNodes = columnsMap[finalRoundNum].orEmpty().filter { it.isThirdPlaceMatch }
        if (thirdNodes.isNotEmpty()) {
            val finalColX = marginX + (colCount - 1) * (colWidth + gapX)
            val finalMainBottom = columnsMap[finalRoundNum].orEmpty()
                .filter { !it.isThirdPlaceMatch }
                .mapNotNull { nodeBoxes[it.baseCode]?.bottom }
                .maxOrNull() ?: (bracketTop + boxHeight)

            thirdNodes.forEachIndexed { idx, node ->
                val preferredTop = (finalMainBottom + 24f + idx * (boxHeight + 8f))
                    .coerceAtMost(bracketBottom - boxHeight)
                val safeTop = if (preferredTop < finalMainBottom + 8f && finalMainBottom + boxHeight + 8f <= bracketBottom) {
                    finalMainBottom + 8f
                } else {
                    preferredTop
                }
                nodeBoxes[node.baseCode] = RectF(finalColX, safeTop, finalColX + colWidth, safeTop + boxHeight)
            }
        }

        // Önce bağlantı çizgilerini çiz (kutuların arkasında temiz görünüm)
        val completedLinePaint = Paint().apply {
            color = COLOR_GREEN
            style = Paint.Style.STROKE
            strokeWidth = 1.6f
            isAntiAlias = true
        }
        val pendingLinePaint = Paint().apply {
            color = 0xFF94A3B8.toInt()
            style = Paint.Style.STROKE
            strokeWidth = 1.1f
            isAntiAlias = true
        }
        val thirdPlaceLinePaint = Paint().apply {
            color = COLOR_GOLD
            style = Paint.Style.STROKE
            strokeWidth = 1.0f
            pathEffect = DashPathEffect(floatArrayOf(4f, 3f), 0f)
            isAntiAlias = true
        }

        val allTreeNodes = treeRoundNumbers.flatMap { columnsMap[it].orEmpty() }
        val nodeLookup = allTreeNodes.associateBy { it.baseCode }

        allTreeNodes.forEachIndexed { _, node ->
            val targetRect = nodeBoxes[node.baseCode] ?: return@forEachIndexed
            val sources = listOf(
                Pair(node.p1SourceBaseCode, targetRect.top + headerStripH + playerRowH * 0.5f),
                Pair(node.p2SourceBaseCode, targetRect.top + headerStripH + playerRowH * 1.5f)
            )
            sources.forEachIndexed { slotIdx, (srcCode, targetY) ->
                if (srcCode != null) {
                    val srcRect = nodeBoxes[srcCode]
                    val srcNode = nodeLookup[srcCode]
                    if (srcRect != null && srcRect.right < targetRect.left) {
                        val startX = srcRect.right
                        val startY = srcRect.centerY()
                        val endX = targetRect.left
                        val offsetFactor = if (slotIdx == 0) 0.44f else 0.56f
                        val midX = startX + (endX - startX) * offsetFactor
                        val path = Path().apply {
                            moveTo(startX, startY)
                            lineTo(midX, startY)
                            lineTo(midX, targetY)
                            lineTo(endX, targetY)
                        }
                        val paint = when {
                            node.isThirdPlaceMatch -> thirdPlaceLinePaint
                            srcNode?.isCompleted == true -> completedLinePaint
                            else -> pendingLinePaint
                        }
                        canvas.drawPath(path, paint)
                    }
                }
            }
        }

        // Tüm eşleşme kutularını çiz
        allTreeNodes.forEach { node ->
            val rect = nodeBoxes[node.baseCode] ?: return@forEach
            drawBracketMatchNode(canvas, node, rect, headerStripH, playerRowH)
        }

        drawFooter(canvas, tournament.name, currentPageNum, LANDSCAPE_W, LANDSCAPE_H)
        pdfDocument.finishPage(page)
        return currentPageNum + 1
    }

    private fun drawOverflowFirstRoundPage(
        pdfDocument: PdfDocument,
        context: Context,
        tournament: Tournament,
        roundNum: Int,
        maxRound: Int,
        hasPreElim: Boolean,
        nodes: List<TournamentOutcomeHelper.BracketMatchupNode>,
        dateStr: String,
        pageNum: Int,
        totalMatches: Int,
        completedMatches: Int
    ): Int {
        val pageInfo = PdfDocument.PageInfo.Builder(LANDSCAPE_W, LANDSCAPE_H, pageNum).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        var y = drawDocumentHeader(
            canvas = canvas,
            context = context,
            tournament = tournament,
            dateStr = dateStr,
            pageWidth = LANDSCAPE_W,
            matches = emptyList(),
            overrideTotalMatches = totalMatches,
            overrideCompletedMatches = completedMatches
        )

        val rTitle = TournamentOutcomeHelper.getRoundDisplayName(roundNum, maxRound, true, hasPreElim)
        val sectionP = Paint().apply {
            color = COLOR_NAVY
            textSize = 10.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("⚡ $rTitle Şeması — Kazananlar Sonraki Sayfadaki Ana Eleme Ağacına Yükselir", 28f, y + 10f, sectionP)
        y += 20f

        val colCount = 2
        val gapX = 24f
        val marginX = 28f
        val colWidth = (LANDSCAPE_W - marginX * 2f - gapX) / colCount
        val rowsPerCol = (nodes.size + 1) / 2
        val availableH = LANDSCAPE_H - 32f - y
        val slotH = (availableH / rowsPerCol.coerceAtLeast(1)).coerceAtMost(56f)
        val boxH = (slotH - 8f).coerceAtLeast(36f)

        nodes.forEachIndexed { idx, node ->
            val col = idx / rowsPerCol
            val row = idx % rowsPerCol
            val x = marginX + col * (colWidth + gapX)
            val top = y + row * slotH
            drawBracketMatchNode(
                canvas = canvas,
                node = node,
                rect = RectF(x, top, x + colWidth, top + boxH),
                headerH = 12f,
                rowH = (boxH - 12f) / 2f
            )
        }

        drawFooter(canvas, tournament.name, pageNum, LANDSCAPE_W, LANDSCAPE_H)
        pdfDocument.finishPage(page)
        return pageNum + 1
    }

    private fun drawBracketMatchNode(
        canvas: Canvas,
        node: TournamentOutcomeHelper.BracketMatchupNode,
        rect: RectF,
        headerH: Float,
        rowH: Float
    ) {
        val cardFill = Paint().apply { color = Color.WHITE; style = Paint.Style.FILL }
        val cardBorder = Paint().apply {
            color = when {
                node.isThirdPlaceMatch -> COLOR_GOLD
                node.isCompleted -> COLOR_GREEN
                else -> COLOR_BORDER
            }
            style = Paint.Style.STROKE
            strokeWidth = if (node.isCompleted || node.isThirdPlaceMatch) 1.2f else 0.9f
        }
        canvas.drawRoundRect(rect, 4f, 4f, cardFill)

        // Üst şerit
        val hdrBg = Paint().apply {
            color = when {
                node.isThirdPlaceMatch -> COLOR_GOLD_BG
                node.isCompleted -> 0xFFE2E8F0.toInt()
                else -> COLOR_ZEBRA
            }
            style = Paint.Style.FILL
        }
        val hdrRect = RectF(rect.left, rect.top, rect.right, rect.top + headerH)
        canvas.drawRect(hdrRect, hdrBg)

        val codeText = if (node.isThirdPlaceMatch) "3.LÜK (${node.baseCode})" else node.baseCode
        val hdrPaint = Paint().apply {
            color = if (node.isThirdPlaceMatch) COLOR_GOLD_DARK else COLOR_NAVY
            textSize = 6.8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText(codeText, rect.left + 4f, rect.top + headerH - 2.5f, hdrPaint)

        val statusBadge = if (node.scoreSummary != "-" && node.scoreSummary != "BAY" && node.scoreSummary != "✓") {
            node.scoreSummary
        } else {
            node.statusLabel
        }
        val statusPaint = Paint().apply {
            color = when {
                node.isBye -> COLOR_TURQUOISE
                node.isCompleted -> COLOR_GREEN_DARK
                else -> COLOR_MUTED
            }
            textSize = 6.4f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val maxBadgeW = rect.width() * 0.56f
        val safeBadge = ellipsizeText(statusBadge, statusPaint, maxBadgeW)
        val bw = statusPaint.measureText(safeBadge)
        canvas.drawText(safeBadge, rect.right - bw - 4f, rect.top + headerH - 2.5f, statusPaint)

        // 1. Oyuncu satırı
        val r1Rect = RectF(rect.left, rect.top + headerH, rect.right, rect.top + headerH + rowH)
        val r2Rect = RectF(rect.left, rect.top + headerH + rowH, rect.right, rect.bottom)

        val p1Won = node.isCompleted && node.winnerId != null && node.winnerId == node.player1Id
        val p2Won = node.isCompleted && node.winnerId != null && node.winnerId == node.player2Id

        val leg1 = node.legs.firstOrNull { !it.isTieBreaker && !it.matchCode.endsWith("-R") }
        val isSingleScore = node.legs.size == 1 && leg1?.score1 != null && leg1.score2 != null

        drawBracketPlayerRow(
            canvas = canvas,
            rect = r1Rect,
            label = node.player1Label,
            isWinner = p1Won,
            isLoser = node.isCompleted && !node.isBye && node.winnerId != null && !p1Won,
            isPlaceholder = TournamentOutcomeHelper.isPlaceholderPlayerId(node.player1Id),
            isBye = node.player1Id == "BYE",
            scoreText = if (isSingleScore) "${leg1?.score1}" else if (p1Won) "✓" else null
        )

        // Satır ayırıcı
        val divPaint = Paint().apply { color = 0xFFE2E8F0.toInt(); strokeWidth = 0.6f }
        canvas.drawLine(rect.left, r1Rect.bottom, rect.right, r1Rect.bottom, divPaint)

        drawBracketPlayerRow(
            canvas = canvas,
            rect = r2Rect,
            label = node.player2Label,
            isWinner = p2Won,
            isLoser = node.isCompleted && !node.isBye && node.winnerId != null && !p2Won,
            isPlaceholder = TournamentOutcomeHelper.isPlaceholderPlayerId(node.player2Id),
            isBye = node.player2Id == "BYE",
            scoreText = if (isSingleScore) "${leg1?.score2}" else if (p2Won) "✓" else null
        )

        // Dış çerçeveyi en üstte çiz
        canvas.drawRoundRect(rect, 4f, 4f, cardBorder)
    }

    private fun drawBracketPlayerRow(
        canvas: Canvas,
        rect: RectF,
        label: String,
        isWinner: Boolean,
        isLoser: Boolean,
        isPlaceholder: Boolean,
        isBye: Boolean,
        scoreText: String?
    ) {
        if (isWinner) {
            val bg = Paint().apply { color = COLOR_GREEN_BG; style = Paint.Style.FILL }
            canvas.drawRect(rect, bg)
        } else if (isLoser) {
            val bg = Paint().apply { color = 0xFFFFF1F2.toInt(); style = Paint.Style.FILL }
            canvas.drawRect(rect, bg)
        }

        val scoreReserve = if (scoreText != null) 18f else 4f
        val textPaint = Paint().apply {
            color = when {
                isWinner -> COLOR_GREEN_DARK
                isLoser -> COLOR_MUTED
                isBye -> COLOR_TURQUOISE
                isPlaceholder -> COLOR_MUTED
                else -> COLOR_TEXT
            }
            textSize = if (rect.height() >= 18f) 7.4f else 6.6f
            typeface = when {
                isWinner -> Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isPlaceholder -> Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
                else -> Typeface.DEFAULT
            }
            isAntiAlias = true
        }
        val availW = (rect.width() - 8f - scoreReserve).coerceAtLeast(24f)
        val clipped = ellipsizeText(label, textPaint, availW)
        val baseline = rect.centerY() + (textPaint.textSize * 0.35f)
        canvas.drawText(clipped, rect.left + 4f, baseline, textPaint)

        if (scoreText != null) {
            val scPaint = Paint().apply {
                color = if (isWinner) COLOR_GREEN_DARK else COLOR_TEXT
                textSize = 7.4f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            val sw = scPaint.measureText(scoreText)
            canvas.drawText(scoreText, rect.right - sw - 4f, baseline, scPaint)
        }
    }

    // -----------------------------------------------------------------
    // 2. ELEME FİKSTÜR & SKOR DETAY SAYFALARI (A4 PORTRAIT)
    // -----------------------------------------------------------------
    private fun drawKnockoutFixtureTablePages(
        pdfDocument: PdfDocument,
        context: Context,
        tournament: Tournament,
        knockoutMatches: List<TournamentMatch>,
        allMatches: List<TournamentMatch>,
        students: List<Student>,
        dateStr: String,
        startPageNum: Int,
        includeFullHeader: Boolean
    ): Int {
        val cursor = PdfCursor(pdfDocument, tournament, dateStr, PORTRAIT_W, PORTRAIT_H)
        cursor.pageNumber = startPageNum - 1
        cursor.startPage(
            continuationTitle = "${tournament.name} - Eleme Fikstürü ve Maç Sonuçları",
            context = context,
            drawFullHeader = includeFullHeader
        )

        val rounds = knockoutMatches.map { it.round }.distinct().sorted()
        val maxRound = rounds.maxOrNull() ?: 1
        val hasPreElim = TournamentOutcomeHelper.hasPreEliminationRound(knockoutMatches)

        val sectionPaint = Paint().apply {
            color = COLOR_NAVY
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        cursor.canvas.drawText("⚔️ TÜM ELEME FİKSTÜRÜ VE GÜNCEL SKORLAR", 36f, cursor.y + 10f, sectionPaint)
        cursor.y += 20f

        rounds.forEach { rNum ->
            val rMatches = knockoutMatches.filter { it.round == rNum }
                .sortedWith(compareBy<TournamentMatch> { if (it.isThirdPlaceMatch) 0 else 1 }.thenBy { it.matchCode })
            val roundLabel = TournamentOutcomeHelper.getRoundDisplayName(rNum, maxRound, true, hasPreElim)

            cursor.ensureSpace(58f, "${tournament.name} - Eleme Fikstürü (Devam)")
            drawRoundBanner(cursor.canvas, roundLabel, rMatches.count { it.status == "completed" }, rMatches.size, cursor.y, PORTRAIT_W)
            cursor.y += 20f
            drawMatchTableHeader(cursor.canvas, cursor.y, PORTRAIT_W)
            cursor.y += 18f

            rMatches.forEachIndexed { idx, match ->
                if (cursor.y + 18f > PORTRAIT_H - 36f) {
                    cursor.startPage(continuationTitle = "${tournament.name} - $roundLabel (Devam)")
                    drawRoundBanner(cursor.canvas, "$roundLabel (Devam)", rMatches.count { it.status == "completed" }, rMatches.size, cursor.y, PORTRAIT_W)
                    cursor.y += 20f
                    drawMatchTableHeader(cursor.canvas, cursor.y, PORTRAIT_W)
                    cursor.y += 18f
                }
                drawMatchTableRow(cursor.canvas, match, allMatches, students, idx, cursor.y, PORTRAIT_W)
                cursor.y += 17f
            }
            cursor.y += 10f
        }

        val lastPage = cursor.pageNumber
        cursor.finishCurrentPage()
        return lastPage + 1
    }

    // -----------------------------------------------------------------
    // 3. LİG RAPORU (GÜNCEL PUAN DURUMU + TÜM HAFTALIK FİKSTÜR)
    // -----------------------------------------------------------------
    private fun drawLeagueReportPages(
        pdfDocument: PdfDocument,
        context: Context,
        tournament: Tournament,
        matches: List<TournamentMatch>,
        standings: List<TournamentStanding>,
        students: List<Student>,
        dateStr: String
    ) {
        val cursor = PdfCursor(pdfDocument, tournament, dateStr, PORTRAIT_W, PORTRAIT_H)
        cursor.startPage(context = context, drawFullHeader = true)

        val sectionPaint = Paint().apply {
            color = COLOR_NAVY
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        if (standings.isNotEmpty()) {
            cursor.canvas.drawText("📊 GÜNCEL LİG PUAN DURUMU", 36f, cursor.y + 8f, sectionPaint)
            cursor.y += 16f
            drawStandingsHeader(cursor.canvas, cursor.y, PORTRAIT_W)
            cursor.y += 18f

            standings.forEachIndexed { idx, st ->
                if (cursor.y + 18f > PORTRAIT_H - 36f) {
                    cursor.startPage("${tournament.name} - Puan Durumu (Devam)")
                    drawStandingsHeader(cursor.canvas, cursor.y, PORTRAIT_W)
                    cursor.y += 18f
                }
                drawStandingsRow(cursor.canvas, idx, st, isAdvancing = false, y = cursor.y, pageWidth = PORTRAIT_W)
                cursor.y += 17f
            }
            cursor.y += 14f
        }

        val rounds = matches.map { it.round }.distinct().sorted()
        if (rounds.isNotEmpty()) {
            cursor.ensureSpace(64f, "${tournament.name} - Lig Fikstürü")
            cursor.canvas.drawText("🗓️ TÜM HAFTALIK LİG FİKSTÜRÜ VE SKORLAR", 36f, cursor.y + 8f, sectionPaint)
            cursor.y += 16f

            rounds.forEach { rNum ->
                val weekMatches = matches.filter { it.round == rNum }.sortedBy { it.matchCode }
                val weekTitle = "$rNum. Hafta Karşılaşmaları"
                cursor.ensureSpace(56f, "${tournament.name} - Lig Fikstürü (Devam)")
                drawRoundBanner(cursor.canvas, weekTitle, weekMatches.count { it.status == "completed" }, weekMatches.size, cursor.y, PORTRAIT_W)
                cursor.y += 20f
                drawMatchTableHeader(cursor.canvas, cursor.y, PORTRAIT_W)
                cursor.y += 18f

                weekMatches.forEachIndexed { idx, match ->
                    if (cursor.y + 18f > PORTRAIT_H - 36f) {
                        cursor.startPage("${tournament.name} - $weekTitle (Devam)")
                        drawRoundBanner(cursor.canvas, "$weekTitle (Devam)", weekMatches.count { it.status == "completed" }, weekMatches.size, cursor.y, PORTRAIT_W)
                        cursor.y += 20f
                        drawMatchTableHeader(cursor.canvas, cursor.y, PORTRAIT_W)
                        cursor.y += 18f
                    }
                    drawMatchTableRow(cursor.canvas, match, matches, students, idx, cursor.y, PORTRAIT_W)
                    cursor.y += 17f
                }
                cursor.y += 8f
            }
        }

        cursor.finishCurrentPage()
    }

    // -----------------------------------------------------------------
    // 4. GRUP VE GRUP+ELEME RAPORU (HER GRUBUN SON MAÇ İTİBARİ İLE PUAN DURUMU + FİKSTÜR)
    // -----------------------------------------------------------------
    private fun drawGroupReportPages(
        pdfDocument: PdfDocument,
        context: Context,
        tournament: Tournament,
        matches: List<TournamentMatch>,
        students: List<Student>,
        dateStr: String,
        isGroupElim: Boolean
    ): Int {
        val cursor = PdfCursor(pdfDocument, tournament, dateStr, PORTRAIT_W, PORTRAIT_H)
        cursor.startPage(context = context, drawFullHeader = true)

        val groupReports = TournamentOutcomeHelper.buildGroupReports(tournament, matches, students)
        val sectionPaint = Paint().apply {
            color = COLOR_NAVY
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        cursor.canvas.drawText(
            if (isGroupElim) "👥 GRUP AŞAMASI: SON MAÇ İTİBARİ İLE PUAN DURUMLARI VE FİKSTÜR"
            else "👥 GRUPLAR: SON MAÇ İTİBARİ İLE PUAN DURUMLARI VE FİKSTÜRLER",
            36f,
            cursor.y + 8f,
            sectionPaint
        )
        cursor.y += 18f

        groupReports.forEach { group ->
            val neededForGroupHeaderAndTopRows = 38f + 18f + (group.standings.size.coerceAtMost(4) * 17f)
            cursor.ensureSpace(neededForGroupHeaderAndTopRows, "${tournament.name} - Grup Puan Durumları")

            // Grup Başlık Şeridi (Son maç bilgisi dahil)
            drawGroupBanner(cursor.canvas, group, isGroupElim, tournament.advancingPerGroup, cursor.y, PORTRAIT_W)
            cursor.y += 30f

            // Grup Puan Durumu Tablosu
            drawStandingsHeader(cursor.canvas, cursor.y, PORTRAIT_W)
            cursor.y += 18f

            group.standings.forEachIndexed { idx, st ->
                if (cursor.y + 18f > PORTRAIT_H - 36f) {
                    cursor.startPage("${tournament.name} - ${group.groupName} Puan Durumu")
                    drawGroupBanner(cursor.canvas, group, isGroupElim, tournament.advancingPerGroup, cursor.y, PORTRAIT_W)
                    cursor.y += 30f
                    drawStandingsHeader(cursor.canvas, cursor.y, PORTRAIT_W)
                    cursor.y += 18f
                }
                val isAdvancing = isGroupElim && idx < tournament.advancingPerGroup
                drawStandingsRow(cursor.canvas, idx, st, isAdvancing = isAdvancing, y = cursor.y, pageWidth = PORTRAIT_W)
                cursor.y += 17f
            }

            cursor.y += 8f

            // Grubun Tüm Fikstürü (Oynanan skorlar + oynanacak maçlar)
            if (group.matches.isNotEmpty()) {
                cursor.ensureSpace(46f, "${tournament.name} - ${group.groupName} Maçları")
                drawRoundBanner(
                    canvas = cursor.canvas,
                    title = "${group.groupName} Fikstür ve Maç Sonuçları",
                    completed = group.completedCount,
                    total = group.matches.size,
                    y = cursor.y,
                    pageWidth = PORTRAIT_W
                )
                cursor.y += 20f
                drawMatchTableHeader(cursor.canvas, cursor.y, PORTRAIT_W)
                cursor.y += 18f

                group.matches.forEachIndexed { idx, m ->
                    if (cursor.y + 18f > PORTRAIT_H - 36f) {
                        cursor.startPage("${tournament.name} - ${group.groupName} Maçları (Devam)")
                        drawRoundBanner(
                            canvas = cursor.canvas,
                            title = "${group.groupName} Fikstür (Devam)",
                            completed = group.completedCount,
                            total = group.matches.size,
                            y = cursor.y,
                            pageWidth = PORTRAIT_W
                        )
                        cursor.y += 20f
                        drawMatchTableHeader(cursor.canvas, cursor.y, PORTRAIT_W)
                        cursor.y += 18f
                    }
                    drawMatchTableRow(cursor.canvas, m, matches, students, idx, cursor.y, PORTRAIT_W)
                    cursor.y += 17f
                }
            }

            cursor.y += 14f
        }

        val lastPage = cursor.pageNumber
        cursor.finishCurrentPage()
        return lastPage + 1
    }

    // -----------------------------------------------------------------
    // ORTAK ÇİZİM BİLEŞENLERİ (BAŞLIK, TABLOLAR, ALT BİLGİ)
    // -----------------------------------------------------------------
    private fun drawDocumentHeader(
        canvas: Canvas,
        context: Context,
        tournament: Tournament,
        dateStr: String,
        pageWidth: Int,
        matches: List<TournamentMatch> = emptyList(),
        overrideTotalMatches: Int? = null,
        overrideCompletedMatches: Int? = null
    ): Float {
        val marginX = if (pageWidth > PORTRAIT_W) 28f else 36f
        val emblem = renderEmblemBitmap(context, 44)
        emblem?.let { canvas.drawBitmap(it, marginX, 26f, null) }

        val textStartX = if (emblem != null) marginX + 52f else marginX

        val brandPaint = Paint().apply {
            color = COLOR_TURQUOISE
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("BİRİZ SINIF & ETKİNLİK YÖNETİM SİSTEMİ", textStartX, 34f, brandPaint)

        val titlePaint = Paint().apply {
            color = COLOR_NAVY
            textSize = 15f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val titleClipped = ellipsizeText("🏆 ${tournament.name}", titlePaint, pageWidth - textStartX - marginX)
        canvas.drawText(titleClipped, textStartX, 52f, titlePaint)

        val subPaint = Paint().apply {
            color = COLOR_MUTED
            textSize = 8.3f
            isAntiAlias = true
        }
        canvas.drawText(
            "Format: ${tournament.type.displayName} (${tournament.matchType.displayName})  |  Durum: ${tournament.status.displayName}  |  İndirme Tarihi: $dateStr",
            textStartX,
            66f,
            subPaint
        )

        val divPaint = Paint().apply { color = COLOR_BORDER; strokeWidth = 1f }
        canvas.drawLine(marginX, 78f, pageWidth - marginX, 78f, divPaint)

        // Özet / Podyum KPI Kutuları
        val totalM = overrideTotalMatches ?: matches.size
        val compM = overrideCompletedMatches ?: matches.count { it.status == "completed" }

        val kpis = if (tournament.status == TournamentStatus.TAMAMLANDI && !tournament.winnerName.isNullOrBlank()) {
            listOf(
                Triple("🥇 ŞAMPİYON", tournament.winnerName ?: "-", COLOR_GOLD_DARK),
                Triple("🥈 İKİNCİ", tournament.runnerUpName ?: "-", COLOR_SILVER),
                Triple("🥉 ÜÇÜNCÜ", tournament.thirdPlaceName ?: "-", 0xFF92400E.toInt())
            )
        } else {
            listOf(
                Triple("KATILIMCI", "${tournament.participants.size} Öğrenci", COLOR_NAVY),
                Triple("TOPLAM FİKSTÜR", "$totalM Karşılaşma", COLOR_TURQUOISE),
                Triple("GÜNCEL DURUM", "$compM / $totalM Tamamlandı", COLOR_GREEN_DARK)
            )
        }

        val kpiY = 86f
        val kpiH = if (pageWidth > PORTRAIT_W) 38f else 46f
        val cardGap = 10f
        val cardW = (pageWidth - marginX * 2f - cardGap * 2f) / 3f

        val fillP = Paint().apply { color = COLOR_ZEBRA; style = Paint.Style.FILL }
        val borderP = Paint().apply { color = COLOR_BORDER; style = Paint.Style.STROKE; strokeWidth = 0.9f }
        val lblP = Paint().apply {
            color = COLOR_MUTED
            textSize = 7.2f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val valP = Paint().apply {
            textSize = if (pageWidth > PORTRAIT_W) 9.8f else 10.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        kpis.forEachIndexed { idx, (lbl, v, c) ->
            val cx = marginX + idx * (cardW + cardGap)
            val r = RectF(cx, kpiY, cx + cardW, kpiY + kpiH)
            canvas.drawRoundRect(r, 5f, 5f, fillP)
            canvas.drawRoundRect(r, 5f, 5f, borderP)
            canvas.drawText(lbl, cx + 8f, kpiY + 13f, lblP)
            valP.color = c
            canvas.drawText(ellipsizeText(v, valP, cardW - 16f), cx + 8f, kpiY + (if (pageWidth > PORTRAIT_W) 29f else 33f), valP)
        }

        return kpiY + kpiH + 12f
    }

    private fun drawContinuationHeader(
        canvas: Canvas,
        tournament: Tournament,
        title: String,
        pageWidth: Int
    ): Float {
        val marginX = if (pageWidth > PORTRAIT_W) 28f else 36f
        val hdrPaint = Paint().apply {
            color = COLOR_NAVY
            textSize = 11.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText(ellipsizeText("🏆 $title", hdrPaint, pageWidth - marginX * 2f), marginX, 34f, hdrPaint)
        val lineP = Paint().apply { color = COLOR_BORDER; strokeWidth = 0.8f }
        canvas.drawLine(marginX, 42f, pageWidth - marginX, 42f, lineP)
        return 54f
    }

    private fun drawGroupBanner(
        canvas: Canvas,
        group: TournamentOutcomeHelper.GroupStandingsReport,
        isGroupElim: Boolean,
        advancingPerGroup: Int,
        y: Float,
        pageWidth: Int
    ) {
        val bg = Paint().apply { color = 0xFFEFF6FF.toInt(); style = Paint.Style.FILL }
        val border = Paint().apply { color = COLOR_TURQUOISE; style = Paint.Style.STROKE; strokeWidth = 0.9f }
        val rect = RectF(36f, y, pageWidth - 36f, y + 26f)
        canvas.drawRoundRect(rect, 5f, 5f, bg)
        canvas.drawRoundRect(rect, 5f, 5f, border)

        val titleP = Paint().apply {
            color = COLOR_NAVY
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val titleStr = if (isGroupElim) {
            "${group.groupName}  •  İlk $advancingPerGroup Play-Off'a Çıkar  (${group.completedCount}/${group.matches.size} Maç)"
        } else {
            "${group.groupName}  (${group.completedCount}/${group.matches.size} Maç Tamamlandı)"
        }
        canvas.drawText(titleStr, 44f, y + 11f, titleP)

        val subP = Paint().apply { color = COLOR_MUTED; textSize = 7.6f; isAntiAlias = true }
        canvas.drawText(ellipsizeText(group.lastCompletedMatchText, subP, pageWidth - 90f), 44f, y + 22f, subP)
    }

    private fun drawRoundBanner(
        canvas: Canvas,
        title: String,
        completed: Int,
        total: Int,
        y: Float,
        pageWidth: Int
    ) {
        val bg = Paint().apply { color = 0xFFF1F5F9.toInt(); style = Paint.Style.FILL }
        val rect = RectF(36f, y, pageWidth - 36f, y + 17f)
        canvas.drawRoundRect(rect, 4f, 4f, bg)

        val titleP = Paint().apply {
            color = COLOR_NAVY
            textSize = 8.8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText(title, 42f, y + 12f, titleP)

        val badgeP = Paint().apply {
            color = if (completed == total && total > 0) COLOR_GREEN_DARK else COLOR_MUTED
            textSize = 7.8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val statusText = "$completed / $total Maç"
        val bw = badgeP.measureText(statusText)
        canvas.drawText(statusText, pageWidth - 42f - bw, y + 12f, badgeP)
    }

    private fun drawStandingsHeader(canvas: Canvas, y: Float, pageWidth: Int) {
        val bg = Paint().apply { color = COLOR_NAVY; style = Paint.Style.FILL }
        canvas.drawRect(36f, y, pageWidth - 36f, y + 18f, bg)

        val th = Paint().apply {
            color = Color.WHITE
            textSize = 8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("Sıra", 42f, y + 12f, th)
        canvas.drawText("Öğrenci", 74f, y + 12f, th)
        canvas.drawText("O", 282f, y + 12f, th)
        canvas.drawText("G", 316f, y + 12f, th)
        canvas.drawText("B", 350f, y + 12f, th)
        canvas.drawText("M", 384f, y + 12f, th)
        canvas.drawText("AG", 418f, y + 12f, th)
        canvas.drawText("YG", 452f, y + 12f, th)
        canvas.drawText("AV", 488f, y + 12f, th)
        canvas.drawText("P", 535f, y + 12f, th)
    }

    private fun drawStandingsRow(
        canvas: Canvas,
        idx: Int,
        st: TournamentStanding,
        isAdvancing: Boolean,
        y: Float,
        pageWidth: Int
    ) {
        val bg = when {
            isAdvancing -> Paint().apply { color = 0xFFECFDF5.toInt(); style = Paint.Style.FILL }
            idx % 2 == 1 -> Paint().apply { color = COLOR_ZEBRA; style = Paint.Style.FILL }
            else -> null
        }
        if (bg != null) {
            canvas.drawRect(36f, y, pageWidth - 36f, y + 17f, bg)
        }

        val rowP = Paint().apply { color = COLOR_TEXT; textSize = 8f; isAntiAlias = true }
        val boldP = Paint(rowP).apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = if (isAdvancing) COLOR_GREEN_DARK else COLOR_NAVY
        }

        val rank = idx + 1
        val rankStr = when (rank) {
            1 -> "🥇 1."
            2 -> "🥈 2."
            3 -> "🥉 3."
            else -> "$rank."
        }
        canvas.drawText(rankStr, 42f, y + 12f, if (isAdvancing) boldP else rowP)
        val nameStr = if (isAdvancing) "⬆️ ${st.playerName}" else st.playerName
        canvas.drawText(ellipsizeText(nameStr, boldP, 198f), 74f, y + 12f, if (idx == 0 || isAdvancing) boldP else rowP)

        canvas.drawText("${st.played}", 283f, y + 12f, rowP)
        canvas.drawText("${st.won}", 317f, y + 12f, rowP)
        canvas.drawText("${st.drawn}", 351f, y + 12f, rowP)
        canvas.drawText("${st.lost}", 385f, y + 12f, rowP)
        canvas.drawText("${st.goalsFor}", 419f, y + 12f, rowP)
        canvas.drawText("${st.goalsAgainst}", 453f, y + 12f, rowP)
        canvas.drawText(if (st.goalDiff > 0) "+${st.goalDiff}" else "${st.goalDiff}", 488f, y + 12f, rowP)
        canvas.drawText("${st.points}", 535f, y + 12f, boldP)
    }

    private fun drawMatchTableHeader(canvas: Canvas, y: Float, pageWidth: Int) {
        val bg = Paint().apply { color = COLOR_TURQUOISE; style = Paint.Style.FILL }
        canvas.drawRect(36f, y, pageWidth - 36f, y + 18f, bg)

        val th = Paint().apply {
            color = Color.WHITE
            textSize = 8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("Kod", 42f, y + 12f, th)
        canvas.drawText("1. Oyuncu", 104f, y + 12f, th)
        canvas.drawText("Skor / Durum", 248f, y + 12f, th)
        canvas.drawText("2. Oyuncu", 316f, y + 12f, th)
        canvas.drawText("Sonuç / Kazanan", 458f, y + 12f, th)
    }

    private fun drawMatchTableRow(
        canvas: Canvas,
        match: TournamentMatch,
        allMatches: List<TournamentMatch>,
        students: List<Student>,
        idx: Int,
        y: Float,
        pageWidth: Int
    ) {
        if (idx % 2 == 1) {
            val zBg = Paint().apply { color = COLOR_ZEBRA; style = Paint.Style.FILL }
            canvas.drawRect(36f, y, pageWidth - 36f, y + 17f, zBg)
        }

        val rowP = Paint().apply { color = COLOR_TEXT; textSize = 7.7f; isAntiAlias = true }
        val boldP = Paint(rowP).apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = COLOR_GREEN_DARK
        }
        val mutedP = Paint(rowP).apply { color = COLOR_MUTED }

        val codeTag = buildString {
            append(match.matchCode)
            if (match.isThirdPlaceMatch) append(" (3.)")
            else if (match.isTieBreaker) append(" (K)")
        }
        canvas.drawText(ellipsizeText(codeTag, rowP, 58f), 42f, y + 12f, rowP)

        val p1Label = TournamentOutcomeHelper.formatMatchSlotLabel(match, true, allMatches, students, compact = false)
        val p2Label = TournamentOutcomeHelper.formatMatchSlotLabel(match, false, allMatches, students, compact = false)
        val isP1Placeholder = TournamentOutcomeHelper.isPlaceholderPlayerId(match.player1Id)
        val isP2Placeholder = TournamentOutcomeHelper.isPlaceholderPlayerId(match.player2Id)

        val isP1Winner = match.status == "completed" && match.winnerId == match.player1Id
        val isP2Winner = match.status == "completed" && match.winnerId == match.player2Id

        canvas.drawText(
            ellipsizeText(p1Label, if (isP1Winner) boldP else rowP, 138f),
            104f,
            y + 12f,
            when {
                isP1Winner -> boldP
                isP1Placeholder -> mutedP
                else -> rowP
            }
        )

        val scoreStr = when {
            match.player1Id == "BYE" || match.player2Id == "BYE" -> "BAY"
            match.status == "completed" && match.score1 != null && match.score2 != null -> "${match.score1} - ${match.score2}"
            match.status == "completed" -> "Oynandı"
            else -> "Oynanacak"
        }
        canvas.drawText(scoreStr, 250f, y + 12f, if (match.status == "completed") boldP else mutedP)

        canvas.drawText(
            ellipsizeText(p2Label, if (isP2Winner) boldP else rowP, 136f),
            316f,
            y + 12f,
            when {
                isP2Winner -> boldP
                isP2Placeholder -> mutedP
                else -> rowP
            }
        )

        val winnerName = when {
            match.player2Id == "BYE" -> p1Label
            match.player1Id == "BYE" -> p2Label
            match.winnerId == match.player1Id -> p1Label
            match.winnerId == match.player2Id -> p2Label
            else -> null
        }
        val isDraw = match.status == "completed" && match.score1 != null && match.score1 == match.score2 && match.winnerId == null
        val resultStr = when {
            winnerName != null -> "🏆 $winnerName"
            isDraw -> "Berabere"
            else -> "-"
        }
        canvas.drawText(
            ellipsizeText(resultStr, if (winnerName != null) boldP else mutedP, pageWidth - 38f - 458f),
            458f,
            y + 12f,
            if (winnerName != null) boldP else mutedP
        )
    }

    private fun drawFooter(
        canvas: Canvas,
        tournamentName: String,
        pageNumber: Int,
        pageWidth: Int,
        pageHeight: Int
    ) {
        val marginX = if (pageWidth > PORTRAIT_W) 28f else 36f
        val footerP = Paint().apply {
            color = COLOR_MUTED
            textSize = 7.8f
            isAntiAlias = true
        }
        canvas.drawText(
            "BİRİZ Akıllı Sınıf & Turnuva Yönetimi  •  $tournamentName  •  Sayfa $pageNumber",
            marginX,
            pageHeight - 16f,
            footerP
        )
    }

    private fun ellipsizeText(text: String, paint: Paint, maxWidth: Float): String {
        if (maxWidth <= 0f) return ""
        if (paint.measureText(text) <= maxWidth) return text
        val ellipsis = ".."
        val ellipsisW = paint.measureText(ellipsis)
        val count = paint.breakText(text, true, (maxWidth - ellipsisW).coerceAtLeast(1f), null)
        return if (count > 0 && count < text.length) text.take(count) + ellipsis else text.take(1)
    }

    private fun renderEmblemBitmap(context: Context, sizePx: Int): Bitmap? {
        return runCatching {
            val drawable = ContextCompat.getDrawable(context, R.drawable.ic_biriz_emblem) ?: return null
            val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
            val c = Canvas(bitmap)
            drawable.setBounds(0, 0, sizePx, sizePx)
            drawable.draw(c)
            bitmap
        }.getOrNull()
    }
}
