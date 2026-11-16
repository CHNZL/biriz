package com.example.ui.components

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.theme.BirizAmber
import com.example.ui.theme.BirizBorder
import com.example.ui.theme.BirizDarkCardBorder
import com.example.ui.theme.BirizDarkCardGlass
import com.example.ui.theme.BirizDarkTextPrimary
import com.example.ui.theme.BirizDarkTextSecondary
import com.example.ui.theme.BirizError
import com.example.ui.theme.BirizNavyPrimary
import com.example.ui.theme.BirizTextSecondary
import com.example.ui.theme.BirizTurquoise
import com.example.util.InstalledAppInfo
import com.example.util.InstalledAppsHelper

private data class QuickEduProgram(
    val emoji: String,
    val title: String,
    val url: String
)

private val PRESET_EDU_PROGRAMS = listOf(
    QuickEduProgram("📐", "GeoGebra", "https://www.geogebra.org/classic"),
    QuickEduProgram("🧪", "PhET Fen", "https://phet.colorado.edu/tr/"),
    QuickEduProgram("🐱", "Scratch", "https://scratch.mit.edu/projects/editor/"),
    QuickEduProgram("🎯", "Wordwall", "https://wordwall.net/tr"),
    QuickEduProgram("🎵", "Music Lab", "https://musiclab.chromeexperiments.com/"),
    QuickEduProgram("🧮", "Toy Theater", "https://toytheater.com/category/math/"),
    QuickEduProgram("🇹🇷", "EBA", "https://www.eba.gov.tr"),
    QuickEduProgram("🌍", "Earth", "https://earth.google.com/web/")
)

private val QUICK_URL_HINTS = listOf(
    "YouTube" to "https://www.youtube.com",
    "Kahoot" to "https://kahoot.it",
    "Wordwall" to "https://wordwall.net",
    "EBA" to "https://www.eba.gov.tr"
)

@Composable
fun SmartBoardCastSection(
    isDarkTheme: Boolean,
    activeCastTitle: String?,
    activeCastType: String?,
    isBroadcasting: Boolean,
    uploadProgress: Float,
    statusMessage: String?,
    isScreenMirrorPaused: Boolean = false,
    screenMirrorPauseReason: String? = null,
    screenMirrorPackageName: String? = null,
    onCastFile: (Uri) -> Unit,
    onCastUrlOrProgram: (url: String, title: String, isProgram: Boolean) -> Unit,
    onStartAppMirror: (packageName: String, appName: String, resultCode: Int, data: Intent) -> Unit = { _, _, _, _ -> },
    onStopCast: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var selectedTab by remember { mutableIntStateOf(0) }
    var urlInput by remember { mutableStateOf("") }
    var urlTitleInput by remember { mutableStateOf("") }
    var programUrlInput by remember { mutableStateOf("") }
    var programNameInput by remember { mutableStateOf("") }

    var installedApps by remember { mutableStateOf<List<InstalledAppInfo>>(emptyList()) }
    var isLoadingApps by remember { mutableStateOf(false) }
    var appSearchQuery by remember { mutableStateOf("") }
    var hasUsageAccess by remember { mutableStateOf(InstalledAppsHelper.hasUsageStatsPermission(context)) }
    var showWebToolsSection by remember { mutableStateOf(false) }

    var pendingMirrorTarget by remember { mutableStateOf<Pair<String, String>?>(null) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasUsageAccess = InstalledAppsHelper.hasUsageStatsPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(selectedTab) {
        if (selectedTab == 2 && installedApps.isEmpty() && !isLoadingApps) {
            isLoadingApps = true
            installedApps = InstalledAppsHelper.loadLaunchableApps(context)
            isLoadingApps = false
        }
    }

    val screenCaptureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val target = pendingMirrorTarget
        pendingMirrorTarget = null
        val data = result.data
        if (result.resultCode == Activity.RESULT_OK && data != null && target != null) {
            onStartAppMirror(target.first, target.second, result.resultCode, data)
        }
    }

    val requestScreenMirrorForApp: (String, String) -> Unit = { pkg, appName ->
        val pm = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as? MediaProjectionManager
        if (pm != null) {
            pendingMirrorTarget = pkg to appName
            screenCaptureLauncher.launch(pm.createScreenCaptureIntent())
        }
    }

    // Sıfır izinli dosya seçici (SAF OpenDocument)
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            onCastFile(uri)
        }
    }

    // Sıfır izinli Android Photo Picker (Fotoğraf & Video)
    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onCastFile(uri)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkTheme) BirizDarkCardGlass else Color(0xFFF8FAFC)
        ),
        border = BorderStroke(1.dp, if (isDarkTheme) BirizDarkCardBorder else BirizBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("📡", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Tahtaya Paylaşım Gönder",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        color = if (isDarkTheme) BirizDarkTextPrimary else BirizNavyPrimary
                    )
                }
                Text(
                    text = "Paylaşım Sayfası",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BirizTurquoise
                )
            }

            // Aktif Yayın Bannerı
            if (!activeCastTitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                val isMirror = activeCastType == "SCREEN_MIRROR"
                val bannerColor = when {
                    isMirror && isScreenMirrorPaused -> BirizAmber.copy(alpha = 0.18f)
                    isMirror -> Color(0xFF059669).copy(alpha = 0.14f)
                    else -> BirizAmber.copy(alpha = 0.16f)
                }
                val bannerBorder = when {
                    isMirror && isScreenMirrorPaused -> BirizAmber.copy(alpha = 0.65f)
                    isMirror -> Color(0xFF059669).copy(alpha = 0.55f)
                    else -> BirizAmber.copy(alpha = 0.55f)
                }
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = bannerColor,
                    border = BorderStroke(1.dp, bannerBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                val typeBadge = when {
                                    isMirror && isScreenMirrorPaused -> "⏸️ Son Görüntü Tahtada Sabit"
                                    isMirror -> "📱 Canlı Uygulama Paylaşımı"
                                    activeCastType == "FILE" -> "📄 Dosya Paylaşımda"
                                    activeCastType == "PROGRAM" -> "🧩 Web Program Paylaşımda"
                                    else -> "🌐 URL Paylaşımda"
                                }
                                Text(
                                    text = typeBadge,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        isMirror && !isScreenMirrorPaused -> if (isDarkTheme) Color(0xFF6EE7B7) else Color(0xFF047857)
                                        else -> if (isDarkTheme) Color(0xFFFCD34D) else Color(0xFFB45309)
                                    }
                                )
                                Text(
                                    text = activeCastTitle,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = if (isDarkTheme) Color.White else BirizNavyPrimary
                                )
                            }
                            Button(
                                onClick = onStopCast,
                                modifier = Modifier.testTag("smart_board_stop_cast_button"),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = BirizError,
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Stop,
                                    contentDescription = "Paylaşımı Durdur",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Durdur", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (isMirror && isScreenMirrorPaused) {
                            if (!screenMirrorPauseReason.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = screenMirrorPauseReason,
                                    fontSize = 10.5.sp,
                                    lineHeight = 14.sp,
                                    color = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary
                                )
                            }
                            if (!screenMirrorPackageName.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = {
                                            requestScreenMirrorForApp(screenMirrorPackageName, activeCastTitle)
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        border = BorderStroke(1.dp, BirizTurquoise)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = "Yeniden Yansıt",
                                            modifier = Modifier.size(14.dp),
                                            tint = BirizTurquoise
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Uygulamayı Yeniden Yansıt",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = BirizTurquoise
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (isBroadcasting) {
                Spacer(modifier = Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { uploadProgress.coerceIn(0.05f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(999.dp)),
                    color = BirizTurquoise,
                    trackColor = BirizTurquoise.copy(alpha = 0.2f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tahtaya gönderiliyor (%${(uploadProgress * 100).toInt().coerceIn(1, 100)})...",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BirizTurquoise
                )
            }

            if (!statusMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = statusMessage,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (statusMessage.startsWith("✅")) Color(0xFF059669) else BirizError
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 3 Sekme: Dosya | URL | Program
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = if (isDarkTheme) Color(0x25FFFFFF) else Color(0xFFE2E8F0).copy(alpha = 0.7f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                CastTabChip(
                    label = "📄 Dosya",
                    selected = selectedTab == 0,
                    isDarkTheme = isDarkTheme,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("cast_tab_file"),
                    onClick = { selectedTab = 0 }
                )
                CastTabChip(
                    label = "🌐 URL",
                    selected = selectedTab == 1,
                    isDarkTheme = isDarkTheme,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("cast_tab_url"),
                    onClick = { selectedTab = 1 }
                )
                CastTabChip(
                    label = "📱 Program",
                    selected = selectedTab == 2,
                    isDarkTheme = isDarkTheme,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("cast_tab_program"),
                    onClick = { selectedTab = 2 }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (selectedTab) {
                0 -> {
                    // TAB 1: Cihazdan Dosya Yayınla
                    Button(
                        onClick = { filePickerLauncher.launch(arrayOf("*/*")) },
                        enabled = !isBroadcasting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("cast_pick_document_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BirizTurquoise,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.AttachFile,
                            contentDescription = "Cihazdan Dosya Seç",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Cihazdan Dosya Seç (PDF, Sunum, Ses, Video...)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = {
                            mediaPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                            )
                        },
                        enabled = !isBroadcasting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("cast_pick_media_button"),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.2.dp, BirizTurquoise.copy(alpha = 0.7f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = "Galeriden Fotoğraf veya Video Seç",
                            modifier = Modifier.size(18.dp),
                            tint = BirizTurquoise
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Galeriden Fotoğraf / Video Seç",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = BirizTurquoise
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Seçilen dosya tahtadaki 'Paylaşım' sayfasında açılır. PDF, resim, ses, video veya diğer dosyaları destekler.",
                        fontSize = 10.5.sp,
                        lineHeight = 14.sp,
                        color = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary
                    )
                }

                1 -> {
                    // TAB 2: URL / Video Yayınla
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { urlInput = it },
                        label = { Text("Web Adresi (URL / YouTube / PDF)") },
                        placeholder = { Text("https://www.youtube.com/watch?v=...") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("cast_url_input"),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = urlTitleInput,
                        onValueChange = { urlTitleInput = it },
                        label = { Text("Tahta Başlığı (İsteğe bağlı)") },
                        placeholder = { Text("Örn: Kesirler Konu Anlatım Videosu") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("cast_url_title_input"),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        QUICK_URL_HINTS.forEach { (label, sampleUrl) ->
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = BirizTurquoise.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, BirizTurquoise.copy(alpha = 0.35f)),
                                modifier = Modifier.clickable {
                                    urlInput = sampleUrl
                                    if (urlTitleInput.isBlank()) urlTitleInput = label
                                }
                            ) {
                                Text(
                                    text = "+ $label",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BirizTurquoise,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            onCastUrlOrProgram(urlInput, urlTitleInput, false)
                        },
                        enabled = urlInput.isNotBlank() && !isBroadcasting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("cast_send_url_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BirizTurquoise,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = "URL Yayınla",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "URL'yi Tahtada Yayınla",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                else -> {
                    // TAB 3: Telefon Program Menüsünden Uygulama Seç ve Canlı Yansıt + Web Eğitim Araçları
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = if (hasUsageAccess) Color(0xFF059669).copy(alpha = 0.10f) else BirizAmber.copy(alpha = 0.12f),
                        border = BorderStroke(
                            1.dp,
                            if (hasUsageAccess) Color(0xFF059669).copy(alpha = 0.35f) else BirizAmber.copy(alpha = 0.45f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f).padding(end = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = "Gizlilik Dondurma Koruması",
                                    tint = if (hasUsageAccess) Color(0xFF059669) else Color(0xFFD97706),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (hasUsageAccess) {
                                        "Otomatik Dondurma Açık: Program dışına çıkınca tahta son karede donar."
                                    } else {
                                        "Programdan çıkınca tahtayı dondurmak için Kullanım Erişimi verebilirsiniz."
                                    },
                                    fontSize = 10.5.sp,
                                    lineHeight = 13.5.sp,
                                    color = if (isDarkTheme) BirizDarkTextPrimary else BirizNavyPrimary
                                )
                            }
                            if (!hasUsageAccess) {
                                OutlinedButton(
                                    onClick = { InstalledAppsHelper.openUsageStatsSettings(context) },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    border = BorderStroke(1.dp, Color(0xFFD97706))
                                ) {
                                    Text(
                                        text = "İzin Ver",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFB45309)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = appSearchQuery,
                        onValueChange = { appSearchQuery = it },
                        placeholder = { Text("Telefondaki programlarda ara...") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Program Ara",
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("cast_app_search_input"),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val filteredApps = remember(installedApps, appSearchQuery) {
                        val q = appSearchQuery.trim().lowercase()
                        if (q.isBlank()) installedApps
                        else installedApps.filter {
                            it.appName.lowercase().contains(q) || it.packageName.lowercase().contains(q)
                        }
                    }

                    if (isLoadingApps) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 18.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = BirizTurquoise
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Telefon uygulamaları yükleniyor...",
                                fontSize = 11.5.sp,
                                color = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary
                            )
                        }
                    } else if (filteredApps.isEmpty()) {
                        Text(
                            text = "Eşleşen uygulama bulunamadı.",
                            fontSize = 11.5.sp,
                            color = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary,
                            modifier = Modifier.padding(vertical = 10.dp)
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 220.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            filteredApps.forEach { app ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable(enabled = !isBroadcasting) {
                                            requestScreenMirrorForApp(app.packageName, app.appName)
                                        }
                                        .testTag("cast_installed_app_${app.packageName}"),
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isDarkTheme) Color(0x22FFFFFF) else Color.White,
                                    border = BorderStroke(
                                        1.dp,
                                        if (isDarkTheme) BirizDarkCardBorder else BirizBorder
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f).padding(end = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (app.icon != null) {
                                                Image(
                                                    bitmap = app.icon,
                                                    contentDescription = app.appName,
                                                    modifier = Modifier
                                                        .size(30.dp)
                                                        .clip(RoundedCornerShape(7.dp))
                                                )
                                            } else {
                                                Box(
                                                    modifier = Modifier
                                                        .size(30.dp)
                                                        .clip(RoundedCornerShape(7.dp))
                                                        .background(BirizTurquoise.copy(alpha = 0.15f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Apps,
                                                        contentDescription = app.appName,
                                                        tint = BirizTurquoise,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = app.appName,
                                                    fontSize = 12.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    color = if (isDarkTheme) BirizDarkTextPrimary else BirizNavyPrimary
                                                )
                                                Text(
                                                    text = "Aç & Yansıt (Çıkınca Dondur)",
                                                    fontSize = 10.sp,
                                                    color = if (isDarkTheme) BirizDarkTextSecondary else BirizTextSecondary
                                                )
                                            }
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = BirizTurquoise.copy(alpha = 0.14f)
                                        ) {
                                            Text(
                                                text = "Yansıt",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = BirizTurquoise,
                                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Açılır Web Eğitim Araçları bölümü
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(9.dp))
                            .clickable { showWebToolsSection = !showWebToolsSection },
                        shape = RoundedCornerShape(9.dp),
                        color = if (isDarkTheme) Color(0x18FFFFFF) else Color(0xFFEFF6FF),
                        border = BorderStroke(1.dp, BirizTurquoise.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "🌐 Hazır Web Eğitim Araçları (GeoGebra, PhET...)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkTheme) BirizDarkTextPrimary else BirizNavyPrimary
                            )
                            Text(
                                text = if (showWebToolsSection) "▲" else "▼",
                                fontSize = 11.sp,
                                color = BirizTurquoise
                            )
                        }
                    }

                    if (showWebToolsSection) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            PRESET_EDU_PROGRAMS.chunked(2).forEach { rowItems ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    rowItems.forEach { prog ->
                                        Surface(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(9.dp))
                                                .clickable(enabled = !isBroadcasting) {
                                                    onCastUrlOrProgram(prog.url, prog.title, true)
                                                }
                                                .testTag("cast_preset_${prog.title}"),
                                            shape = RoundedCornerShape(9.dp),
                                            color = if (isDarkTheme) Color(0x2000B4D8) else Color.White,
                                            border = BorderStroke(
                                                1.dp,
                                                if (isDarkTheme) BirizDarkCardBorder else BirizBorder
                                            )
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(prog.emoji, fontSize = 14.sp)
                                                Spacer(modifier = Modifier.width(5.dp))
                                                Text(
                                                    text = prog.title,
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    color = if (isDarkTheme) BirizDarkTextPrimary else BirizNavyPrimary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = programNameInput,
                            onValueChange = { programNameInput = it },
                            label = { Text("Özel Web Programı Adı") },
                            placeholder = { Text("Örn: Zoom, Wordwall") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("cast_program_name_input"),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = programUrlInput,
                            onValueChange = { programUrlInput = it },
                            label = { Text("Program Web Linki") },
                            placeholder = { Text("https://...") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("cast_program_url_input"),
                            shape = RoundedCornerShape(10.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                onCastUrlOrProgram(programUrlInput, programNameInput, true)
                            },
                            enabled = programUrlInput.isNotBlank() && !isBroadcasting,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("cast_send_program_button"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BirizTurquoise,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Programı Yayınla",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Web Programını Tahtada Aç", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CastTabChip(
    label: String,
    selected: Boolean,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                when {
                    selected -> BirizNavyPrimary
                    isDarkTheme -> Color.Transparent
                    else -> Color.Transparent
                }
            )
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            color = when {
                selected -> Color.White
                isDarkTheme -> BirizDarkTextPrimary
                else -> BirizNavyPrimary
            }
        )
    }
}
