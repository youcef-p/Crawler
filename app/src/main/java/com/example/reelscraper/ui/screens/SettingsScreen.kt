package com.example.reelscraper.ui.screens

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.reelscraper.ui.viewmodel.SettingsViewModel
import com.example.ui.theme.CinemaBlack
import com.example.ui.theme.CinemaSurface
import com.example.ui.theme.CinemaSurfaceVariant
import com.example.ui.theme.CoralPink
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.VividViolet
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf<String?>(null) }
    var showImportDialog by remember { mutableStateOf(false) }
    var importJsonText by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.message.collectLatest { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            containerColor = CinemaSurface,
            title = { Text("Clear All Scraped Media?", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("This will permanently remove all indexed videos, streams, and favorites from local database.", color = Color.LightGray) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllMedia()
                        showClearConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CoralPink, contentColor = Color.White)
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancel", color = Color.LightGray)
                }
            }
        )
    }

    if (showExportDialog != null) {
        AlertDialog(
            onDismissRequest = { showExportDialog = null },
            containerColor = CinemaSurface,
            title = { Text("Exported JSON Database", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("JSON payload ready. You can copy it to clipboard:", color = Color.LightGray, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = CinemaBlack,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(150.dp)
                    ) {
                        Text(
                            text = showExportDialog ?: "",
                            color = NeonCyan,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(8.dp).verticalScroll(rememberScrollState())
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = android.content.ClipData.newPlainText("ReelScraper JSON", showExportDialog)
                        clipboard.setPrimaryClip(clip)
                        showExportDialog = null
                        scope.launch { snackbarHostState.showSnackbar("JSON copied to clipboard!") }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = CinemaBlack)
                ) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copy to Clipboard")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = null }) {
                    Text("Close", color = Color.LightGray)
                }
            }
        )
    }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            containerColor = CinemaSurface,
            title = { Text("Import Database from JSON", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Paste JSON array of media items below:", color = Color.LightGray, fontSize = 12.sp)
                    OutlinedTextField(
                        value = importJsonText,
                        onValueChange = { importJsonText = it },
                        placeholder = { Text("[{\"url\": \"...\", \"title\": \"...\"}]", color = Color.Gray, fontSize = 11.sp) },
                        modifier = Modifier.fillMaxWidth().height(150.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (importJsonText.isNotBlank()) {
                            viewModel.importDatabaseJson(importJsonText)
                            importJsonText = ""
                            showImportDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = CinemaBlack)
                ) {
                    Text("Import")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Cancel", color = Color.LightGray)
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CinemaBlack)
            .statusBarsPadding()
    ) {
        SnackbarHost(hostState = snackbarHostState)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = "Preferences & Settings",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // 1. Crawling Settings Card
            SettingsSectionHeader(title = "Crawling & Discovery", icon = Icons.Default.Radar)
            Card(
                colors = CardDefaults.cardColors(containerColor = CinemaSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Default Scan Level Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Default Scan Levels", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text("Level ${settings.defaultScanLevels}", color = NeonCyan, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = settings.defaultScanLevels.toFloat(),
                            onValueChange = { viewModel.updateScanLevels(it.toInt()) },
                            valueRange = 1f..10f,
                            steps = 8,
                            colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("1 (Fast)", fontSize = 10.sp, color = Color.Gray)
                            Text("10 (Deep Crawl)", fontSize = 10.sp, color = Color.Gray)
                        }
                    }

                    // Format Toggles
                    Text("Allowed Media Formats", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FormatCheckbox("MP4", settings.extractMp4) {
                            viewModel.updateFormatToggles(it, settings.extractWebm, settings.extractHls, settings.extractDash, settings.extractGif)
                        }
                        FormatCheckbox("WEBM", settings.extractWebm) {
                            viewModel.updateFormatToggles(settings.extractMp4, it, settings.extractHls, settings.extractDash, settings.extractGif)
                        }
                        FormatCheckbox("HLS (m3u8)", settings.extractHls) {
                            viewModel.updateFormatToggles(settings.extractMp4, settings.extractWebm, it, settings.extractDash, settings.extractGif)
                        }
                        FormatCheckbox("DASH (mpd)", settings.extractDash) {
                            viewModel.updateFormatToggles(settings.extractMp4, settings.extractWebm, settings.extractHls, it, settings.extractGif)
                        }
                        FormatCheckbox("GIF", settings.extractGif) {
                            viewModel.updateFormatToggles(settings.extractMp4, settings.extractWebm, settings.extractHls, settings.extractDash, it)
                        }
                    }

                    // Max Concurrent Requests & Delay
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Max Concurrent Requests", color = Color.White, fontSize = 13.sp)
                            Text("${settings.maxConcurrentRequests} workers (Limit 1-5)", color = Color.Gray, fontSize = 11.sp)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf(1, 3, 5).forEach { num ->
                                FilterChip(
                                    selected = settings.maxConcurrentRequests == num,
                                    onClick = { viewModel.updateCrawlLimits(settings.maxLinksPerPage, num, settings.pageTimeoutSeconds, settings.requestDelayMs) },
                                    label = { Text("$num") },
                                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = VividViolet, selectedLabelColor = Color.White)
                                )
                            }
                        }
                    }
                }
            }

            // 2. Advanced Extraction Engines Card
            SettingsSectionHeader(title = "Extraction Pipeline Engines", icon = Icons.Default.Tune)
            Card(
                colors = CardDefaults.cardColors(containerColor = CinemaSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SettingToggleRow("HTML Media Tags (<video>, <source>, <a>)", settings.enableHtmlTag) {
                        viewModel.updateAdvancedExtraction(it, settings.enableMetaTags, settings.enableJsonLd, settings.enableInlineScript, settings.enableRegexScan, settings.enableAttributeScan, settings.enableIframeScan, settings.enableFeedSitemapScan, settings.enableJsonApiScan, settings.enableWebViewFallback, settings.webViewOnlyWhenNoMedia, settings.enableContentTypeSniffing)
                    }
                    SettingToggleRow("Meta & Open Graph Tags (og:video, twitter)", settings.enableMetaTags) {
                        viewModel.updateAdvancedExtraction(settings.enableHtmlTag, it, settings.enableJsonLd, settings.enableInlineScript, settings.enableRegexScan, settings.enableAttributeScan, settings.enableIframeScan, settings.enableFeedSitemapScan, settings.enableJsonApiScan, settings.enableWebViewFallback, settings.webViewOnlyWhenNoMedia, settings.enableContentTypeSniffing)
                    }
                    SettingToggleRow("JSON-LD & Structured Data (VideoObject)", settings.enableJsonLd) {
                        viewModel.updateAdvancedExtraction(settings.enableHtmlTag, settings.enableMetaTags, it, settings.enableInlineScript, settings.enableRegexScan, settings.enableAttributeScan, settings.enableIframeScan, settings.enableFeedSitemapScan, settings.enableJsonApiScan, settings.enableWebViewFallback, settings.webViewOnlyWhenNoMedia, settings.enableContentTypeSniffing)
                    }
                    SettingToggleRow("Inline Script & Player Configs (JW, Video.js)", settings.enableInlineScript) {
                        viewModel.updateAdvancedExtraction(settings.enableHtmlTag, settings.enableMetaTags, settings.enableJsonLd, it, settings.enableRegexScan, settings.enableAttributeScan, settings.enableIframeScan, settings.enableFeedSitemapScan, settings.enableJsonApiScan, settings.enableWebViewFallback, settings.webViewOnlyWhenNoMedia, settings.enableContentTypeSniffing)
                    }
                    SettingToggleRow("Regex URL & Protocol Scanning", settings.enableRegexScan) {
                        viewModel.updateAdvancedExtraction(settings.enableHtmlTag, settings.enableMetaTags, settings.enableJsonLd, settings.enableInlineScript, it, settings.enableAttributeScan, settings.enableIframeScan, settings.enableFeedSitemapScan, settings.enableJsonApiScan, settings.enableWebViewFallback, settings.webViewOnlyWhenNoMedia, settings.enableContentTypeSniffing)
                    }
                    SettingToggleRow("Data Attributes (data-src, data-video)", settings.enableAttributeScan) {
                        viewModel.updateAdvancedExtraction(settings.enableHtmlTag, settings.enableMetaTags, settings.enableJsonLd, settings.enableInlineScript, settings.enableRegexScan, it, settings.enableIframeScan, settings.enableFeedSitemapScan, settings.enableJsonApiScan, settings.enableWebViewFallback, settings.webViewOnlyWhenNoMedia, settings.enableContentTypeSniffing)
                    }
                    SettingToggleRow("Iframe & Embed Player Discovery", settings.enableIframeScan) {
                        viewModel.updateAdvancedExtraction(settings.enableHtmlTag, settings.enableMetaTags, settings.enableJsonLd, settings.enableInlineScript, settings.enableRegexScan, settings.enableAttributeScan, it, settings.enableFeedSitemapScan, settings.enableJsonApiScan, settings.enableWebViewFallback, settings.webViewOnlyWhenNoMedia, settings.enableContentTypeSniffing)
                    }
                    SettingToggleRow("RSS / Atom Feed & Sitemap Scanning", settings.enableFeedSitemapScan) {
                        viewModel.updateAdvancedExtraction(settings.enableHtmlTag, settings.enableMetaTags, settings.enableJsonLd, settings.enableInlineScript, settings.enableRegexScan, settings.enableAttributeScan, settings.enableIframeScan, it, settings.enableJsonApiScan, settings.enableWebViewFallback, settings.webViewOnlyWhenNoMedia, settings.enableContentTypeSniffing)
                    }
                    SettingToggleRow("JSON API Endpoint Scanning", settings.enableJsonApiScan) {
                        viewModel.updateAdvancedExtraction(settings.enableHtmlTag, settings.enableMetaTags, settings.enableJsonLd, settings.enableInlineScript, settings.enableRegexScan, settings.enableAttributeScan, settings.enableIframeScan, settings.enableFeedSitemapScan, it, settings.enableWebViewFallback, settings.webViewOnlyWhenNoMedia, settings.enableContentTypeSniffing)
                    }
                    SettingToggleRow("WebView Fallback (Dynamic JS Rendering)", settings.enableWebViewFallback) {
                        viewModel.updateAdvancedExtraction(settings.enableHtmlTag, settings.enableMetaTags, settings.enableJsonLd, settings.enableInlineScript, settings.enableRegexScan, settings.enableAttributeScan, settings.enableIframeScan, settings.enableFeedSitemapScan, settings.enableJsonApiScan, it, settings.webViewOnlyWhenNoMedia, settings.enableContentTypeSniffing)
                    }
                    SettingToggleRow("Content-Type Sniffing (HEAD/Range GET)", settings.enableContentTypeSniffing) {
                        viewModel.updateAdvancedExtraction(settings.enableHtmlTag, settings.enableMetaTags, settings.enableJsonLd, settings.enableInlineScript, settings.enableRegexScan, settings.enableAttributeScan, settings.enableIframeScan, settings.enableFeedSitemapScan, settings.enableJsonApiScan, settings.enableWebViewFallback, settings.webViewOnlyWhenNoMedia, it)
                    }
                }
            }

            // 3. Playback Preferences Card
            SettingsSectionHeader(title = "Player & Feed Behavior", icon = Icons.Default.PlayCircle)
            Card(
                colors = CardDefaults.cardColors(containerColor = CinemaSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SettingToggleRow("Autoplay Next Media on Scroll", settings.autoplayNext) {
                        viewModel.updatePlaybackSettings(it, settings.muteByDefault, settings.preloadAdjacentCount, settings.pauseOnBackground)
                    }
                    SettingToggleRow("Mute Audio by Default", settings.muteByDefault) {
                        viewModel.updatePlaybackSettings(settings.autoplayNext, it, settings.preloadAdjacentCount, settings.pauseOnBackground)
                    }
                    SettingToggleRow("Pause Playback on Background", settings.pauseOnBackground) {
                        viewModel.updatePlaybackSettings(settings.autoplayNext, settings.muteByDefault, settings.preloadAdjacentCount, it)
                    }
                }
            }

            // 4. Data Management Card
            SettingsSectionHeader(title = "Data & Database Tools", icon = Icons.Default.DeleteSweep)
            Card(
                colors = CardDefaults.cardColors(containerColor = CinemaSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { viewModel.clearDuplicateMedia() },
                        colors = ButtonDefaults.buttonColors(containerColor = VividViolet, contentColor = Color.White),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Deduplicate Database (Clear Duplicates)", fontWeight = FontWeight.Bold)
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = { viewModel.exportDatabaseJson { json -> showExportDialog = json } },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.FileUpload, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export JSON", color = NeonCyan, fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = { showImportDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Import JSON", color = NeonCyan, fontSize = 12.sp)
                        }
                    }

                    Button(
                        onClick = { showClearConfirmDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = CoralPink.copy(alpha = 0.85f), contentColor = Color.White),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Clear All Indexed Media", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // 5. About Section Card
            SettingsSectionHeader(title = "About ReelScraper", icon = Icons.Default.Info)
            Card(
                colors = CardDefaults.cardColors(containerColor = CinemaSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("ReelScraper Engine v1.0", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(
                        text = "Native high-performance media discovery pipeline and vertical player for MP4, WebM, HLS, DASH, and animated GIFs.",
                        color = Color.LightGray,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun SettingsSectionHeader(title: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(top = 8.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
        Text(text = title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

@Composable
fun SettingToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = Color.LightGray,
            fontSize = 12.sp,
            modifier = Modifier.weight(1f).padding(end = 8.dp)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = NeonCyan,
                checkedTrackColor = VividViolet,
                uncheckedThumbColor = Color.LightGray,
                uncheckedTrackColor = CinemaSurfaceVariant
            )
        )
    }
}

@Composable
fun FormatCheckbox(label: String, selected: Boolean, onSelectedChange: (Boolean) -> Unit) {
    FilterChip(
        selected = selected,
        onClick = { onSelectedChange(!selected) },
        label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = NeonCyan,
            selectedLabelColor = CinemaBlack,
            labelColor = Color.LightGray
        )
    )
}
