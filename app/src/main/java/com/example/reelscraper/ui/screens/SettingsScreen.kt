package com.example.reelscraper.ui.screens

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Difference
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stream
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
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
import androidx.compose.material3.Scaffold
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
import androidx.compose.runtime.mutableFloatStateOf
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
import com.example.reelscraper.data.settings.AppSettings
import com.example.reelscraper.player.WebVttCue
import com.example.reelscraper.ui.components.SubtitleOverlay
import com.example.reelscraper.ui.viewmodel.SettingsViewModel
import com.example.ui.theme.CinemaBlack
import com.example.ui.theme.CinemaSurface
import com.example.ui.theme.CinemaSurfaceVariant
import com.example.ui.theme.CoralPink
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.VividViolet
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateToProfiles: () -> Unit = {},
    onNavigateToDuplicates: () -> Unit = {},
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
            title = {
                Text("Clear All Media?", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "This will delete all scanned and captured streams from the local database. This action cannot be undone.",
                    color = Color.LightGray
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllMedia()
                        showClearConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CoralPink)
                ) {
                    Text("Delete Everything", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancel", color = NeonCyan)
                }
            }
        )
    }

    if (showExportDialog != null) {
        AlertDialog(
            onDismissRequest = { showExportDialog = null },
            containerColor = CinemaSurface,
            title = { Text("Export Library JSON", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Copy your exported media items:", color = Color.LightGray, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = showExportDialog ?: "",
                        onValueChange = {},
                        readOnly = true,
                        maxLines = 6,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = android.content.ClipData.newPlainText("ReelScraper JSON", showExportDialog)
                        clipboard.setPrimaryClip(clip)
                        showExportDialog = null
                        scope.launch { snackbarHostState.showSnackbar("Copied JSON to clipboard") }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = CinemaBlack)
                ) {
                    Text("Copy to Clipboard")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = null }) {
                    Text("Close", color = Color.White)
                }
            }
        )
    }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            containerColor = CinemaSurface,
            title = { Text("Import Library JSON", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Paste JSON containing exported media items:", color = Color.LightGray, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = importJsonText,
                        onValueChange = { importJsonText = it },
                        placeholder = { Text("Paste JSON here...") },
                        maxLines = 6,
                        modifier = Modifier.fillMaxWidth()
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
                    Text("Cancel", color = Color.White)
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = CinemaBlack,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Settings & Engine",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Discovery controls
            SettingsSectionCard(title = "Smart Discovery", icon = Icons.Default.Radar) {
                SettingsToggleRow(
                    title = "Stay on same domain",
                    subtitle = "Avoid crawling unrelated external websites",
                    checked = settings.sameDomainOnly,
                    onCheckedChange = { viewModel.updateDiscoverySettings(it, settings.includeSubdomains, settings.discoverSitemaps, settings.discoverMediaFromLinkPreloads, settings.followPaginationLinks) }
                )
                Spacer(modifier = Modifier.height(10.dp))
                SettingsToggleRow(
                    title = "Include subdomains",
                    subtitle = "Also follow media and pages on subdomains of the seed site",
                    checked = settings.includeSubdomains,
                    onCheckedChange = { viewModel.updateDiscoverySettings(settings.sameDomainOnly, it, settings.discoverSitemaps, settings.discoverMediaFromLinkPreloads, settings.followPaginationLinks) }
                )
                Spacer(modifier = Modifier.height(10.dp))
                SettingsToggleRow(
                    title = "Discover sitemap URLs",
                    subtitle = "Use robots.txt and common sitemap locations to find media pages",
                    checked = settings.discoverSitemaps,
                    onCheckedChange = { viewModel.updateDiscoverySettings(settings.sameDomainOnly, settings.includeSubdomains, it, settings.discoverMediaFromLinkPreloads, settings.followPaginationLinks) }
                )
                Spacer(modifier = Modifier.height(10.dp))
                SettingsToggleRow(
                    title = "Scan preload/media links",
                    subtitle = "Inspect preload, source, poster and media-related link attributes",
                    checked = settings.discoverMediaFromLinkPreloads,
                    onCheckedChange = { viewModel.updateDiscoverySettings(settings.sameDomainOnly, settings.includeSubdomains, settings.discoverSitemaps, it, settings.followPaginationLinks) }
                )
                Spacer(modifier = Modifier.height(10.dp))
                SettingsToggleRow(
                    title = "Follow pagination",
                    subtitle = "Follow next-page and pagination links while crawling",
                    checked = settings.followPaginationLinks,
                    onCheckedChange = { viewModel.updateDiscoverySettings(settings.sameDomainOnly, settings.includeSubdomains, settings.discoverSitemaps, settings.discoverMediaFromLinkPreloads, it) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 1. Advanced Playback Engine Section
            SettingsSectionCard(title = "Advanced Playback Engine", icon = Icons.Default.Speed) {
                // 60fps Converter
                SettingsToggleRow(
                    title = "60 FPS Video Converter",
                    subtitle = "Align display cadence and video render pacing to 60fps for ultra-smooth reels",
                    checked = settings.enable60FpsConverter,
                    onCheckedChange = { viewModel.update60FpsConverter(it) }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Audio Normalization
                SettingsToggleRow(
                    title = "Audio Loudness Normalization",
                    subtitle = "RMS loudness compression to prevent sudden loud jumps between scraped clips",
                    checked = settings.audioNormalization,
                    onCheckedChange = { viewModel.updateAudioNormalization(it, settings.normalizationStrength) }
                )

                if (settings.audioNormalization) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Normalization Strength:", color = Color.White, fontSize = 12.sp)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        listOf("LIGHT", "NORMAL", "STRONG").forEach { strength ->
                            FilterChip(
                                selected = settings.normalizationStrength.equals(strength, ignoreCase = true),
                                onClick = { viewModel.updateAudioNormalization(true, strength) },
                                label = { Text(strength.lowercase().replaceFirstChar { it.uppercase() }, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NeonCyan,
                                    selectedLabelColor = CinemaBlack,
                                    labelColor = Color.LightGray
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Ambient Mode
                Text("Cinematic Ambient Mode:", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    listOf(
                        "OFF" to "Off",
                        "POSTER_ONLY" to "Poster",
                        "DYNAMIC_LOW" to "Low Freq",
                        "DYNAMIC_HIGH" to "High Freq"
                    ).forEach { (mode, label) ->
                        FilterChip(
                            selected = settings.ambientMode == mode,
                            onClick = { viewModel.updateAmbientMode(mode) },
                            label = { Text(label, fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = VividViolet,
                                selectedLabelColor = Color.White,
                                labelColor = Color.LightGray
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Custom ABR & Data Saver
                SettingsToggleRow(
                    title = "Data Saver Mode",
                    subtitle = "Restricts startup resolution to 480p and disables background prefetching",
                    checked = settings.dataSaverMode,
                    onCheckedChange = { viewModel.updateQualityAndDataSaver(settings.qualityPreference, it) }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // HUD Player Debug Overlay
                SettingsToggleRow(
                    title = "Player Diagnostics HUD",
                    subtitle = "Overlay real-time resolution, bitrate, bandwidth, dropped frames, and rebuffer count",
                    checked = settings.showPlayerDebugStats,
                    onCheckedChange = {
                        viewModel.updatePlaybackSettings(
                            settings.autoplayNext, settings.muteByDefault, settings.preloadAdjacentCount,
                            settings.pauseOnBackground, settings.resumePlayback, it
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. On-Device Intelligence Section
            SettingsSectionCard(title = "On-Device Intelligence", icon = Icons.Default.Psychology) {
                // Smart Reframe
                Text("Smart Reframe for Vertical Feed:", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    text = "Automatically pans landscape videos to focus on subject or motion center",
                    color = Color.LightGray,
                    fontSize = 11.sp
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    listOf(
                        "OFF" to "Off",
                        "CENTER_CROP" to "Center Crop",
                        "MOTION_TRACKING" to "Motion Tracking",
                        "SUBJECT_TRACKING" to "Subject ML"
                    ).forEach { (mode, label) ->
                        FilterChip(
                            selected = settings.smartReframeMode == mode,
                            onClick = { viewModel.updateSmartReframe(mode) },
                            label = { Text(label, fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeonCyan,
                                selectedLabelColor = CinemaBlack,
                                labelColor = Color.LightGray
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Auto Chapters
                SettingsToggleRow(
                    title = "Automatic Local Chapters",
                    subtitle = "Detect scene boundaries and frame histogram differences to segment long videos",
                    checked = settings.enableAutoChapters,
                    onCheckedChange = {
                        viewModel.updateChaptersAndTranscription(
                            it, settings.enableLocalTranscription, settings.transcriptionLanguage, settings.transcriptionQuality
                        )
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Local Transcription
                SettingsToggleRow(
                    title = "Local Speech-to-Text Subtitles",
                    subtitle = "Transcribe video audio into WebVTT cues on-device using Android speech recognizer",
                    checked = settings.enableLocalTranscription,
                    onCheckedChange = {
                        viewModel.updateChaptersAndTranscription(
                            settings.enableAutoChapters, it, settings.transcriptionLanguage, settings.transcriptionQuality
                        )
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Perceptual Duplicate Detection Policy
                Text("Visual Duplicate Detection (pHash):", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    listOf(
                        "DISABLED" to "Off",
                        "GIFS_ONLY" to "GIFs Only",
                        "SHORT_VIDEOS" to "Short Videos",
                        "ALL" to "All Media"
                    ).forEach { (mode, label) ->
                        FilterChip(
                            selected = settings.pHashMode == mode,
                            onClick = { viewModel.updatePhashMode(mode) },
                            label = { Text(label, fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = VividViolet,
                                selectedLabelColor = Color.White,
                                labelColor = Color.LightGray
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Open Duplicate Manager Button
                Button(
                    onClick = onNavigateToDuplicates,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = VividViolet.copy(alpha = 0.3f),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().testTag("open_duplicate_manager_button")
                ) {
                    Icon(Icons.Default.Difference, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Open Visual Duplicate Manager", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Subtitle Styling & Live Preview Section
            SettingsSectionCard(title = "Subtitle Styling & Preview", icon = Icons.Default.Subtitles) {
                // Live preview box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp)
                        .background(Color.DarkGray.copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    SubtitleOverlay(
                        currentCue = WebVttCue(
                            startTimeMs = 0L,
                            endTimeMs = 10000L,
                            text = "Sample Subtitle: ReelScraper 60fps local player"
                        ),
                        settings = settings
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text("Font Size: ${settings.subtitleFontSizeSp} sp", color = Color.White, fontSize = 12.sp)
                Slider(
                    value = settings.subtitleFontSizeSp.toFloat(),
                    onValueChange = {
                        viewModel.updateSubtitleStyling(
                            it.toInt(), settings.subtitleTextColorHex, settings.subtitleBgColorHex, settings.subtitleVerticalOffsetDp
                        )
                    },
                    valueRange = 12f..32f,
                    colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text("Vertical Offset: ${settings.subtitleVerticalOffsetDp} dp", color = Color.White, fontSize = 12.sp)
                Slider(
                    value = settings.subtitleVerticalOffsetDp.toFloat(),
                    onValueChange = {
                        viewModel.updateSubtitleStyling(
                            settings.subtitleFontSizeSp, settings.subtitleTextColorHex, settings.subtitleBgColorHex, it.toInt()
                        )
                    },
                    valueRange = 0f..100f,
                    colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Feed & Predictive Preloading Section
            SettingsSectionCard(title = "Feed & Preloading", icon = Icons.Default.PlayCircle) {
                Text("Predictive Preload Distance: ${settings.preloadAdjacentCount} items", color = Color.White, fontSize = 12.sp)
                Slider(
                    value = settings.preloadAdjacentCount.toFloat(),
                    onValueChange = {
                        viewModel.updatePlaybackSettings(
                            settings.autoplayNext, settings.muteByDefault, it.toInt(),
                            settings.pauseOnBackground, settings.resumePlayback, settings.showPlayerDebugStats
                        )
                    },
                    valueRange = 0f..3f,
                    steps = 2,
                    colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan)
                )

                SettingsToggleRow(
                    title = "Collect Watch Heatmap",
                    subtitle = "Tracks replay activity in 5-second buckets to render scrubber heatmaps",
                    checked = settings.enableHeatmapCollection,
                    onCheckedChange = {
                        viewModel.updatePlaybackSettings(
                            settings.autoplayNext, settings.muteByDefault, settings.preloadAdjacentCount,
                            settings.pauseOnBackground, settings.resumePlayback, settings.showPlayerDebugStats
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 5. Site Profiles & Database Section
            SettingsSectionCard(title = "Profiles & Data Management", icon = Icons.Default.Layers) {
                Button(
                    onClick = onNavigateToProfiles,
                    colors = ButtonDefaults.buttonColors(containerColor = VividViolet, contentColor = Color.White),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().testTag("manage_profiles_button")
                ) {
                    Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Manage Site Profiles & Regex Rules", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.exportDatabaseJson { showExportDialog = it } },
                        modifier = Modifier.weight(1f).testTag("export_json_button")
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp), tint = NeonCyan)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Export JSON", color = Color.White, fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = { showImportDialog = true },
                        modifier = Modifier.weight(1f).testTag("import_json_button")
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp), tint = NeonCyan)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Import JSON", color = Color.White, fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.clearDuplicateMedia() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(16.dp), tint = NeonCyan)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Dedup URLs", color = Color.White, fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = { viewModel.clearBrokenMedia() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp), tint = CoralPink)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clean Broken", color = Color.White, fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { showClearConfirmDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = CoralPink.copy(alpha = 0.2f), contentColor = CoralPink),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().testTag("clear_all_media_button")
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Clear All Indexed Media", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CinemaSurface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(text = title, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(text = subtitle, color = Color.Gray, fontSize = 11.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = NeonCyan,
                checkedTrackColor = NeonCyan.copy(alpha = 0.5f),
                uncheckedThumbColor = Color.LightGray,
                uncheckedTrackColor = CinemaSurfaceVariant
            )
        )
    }
}
