package com.example.reelscraper.ui.screens

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.reelscraper.ui.components.CrawlDepthDialog
import com.example.reelscraper.ui.viewmodel.SampleSite
import com.example.reelscraper.ui.viewmodel.ScrapeUiState
import com.example.reelscraper.ui.viewmodel.ScraperViewModel
import com.example.ui.theme.CinemaBlack
import com.example.ui.theme.CinemaSurface
import com.example.ui.theme.CinemaSurfaceVariant
import com.example.ui.theme.CoralPink
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.VividViolet

@Composable
fun ScraperScreen(
    viewModel: ScraperViewModel,
    onNavigateToFeed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    if (state.showDepthDialog) {
        CrawlDepthDialog(
            initialLevels = state.scanLevels,
            targetUrl = state.targetUrl,
            onConfirm = { levels ->
                viewModel.onConfirmScanWithLevels(levels)
            },
            onDismiss = {
                viewModel.onDismissDepthDialog()
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CinemaBlack)
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Web Media Scraper",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Crawl up to 10 levels deep & extract playable media",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.LightGray
                    )
                }

                Surface(
                    color = VividViolet.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayCircle,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${state.indexedCount} indexed",
                            color = NeonCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // URL Input Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CinemaSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Target Webpage URL",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )

                    OutlinedTextField(
                        value = state.targetUrl,
                        onValueChange = { viewModel.onUrlChanged(it) },
                        placeholder = { Text("https://example.com/videos", color = Color.Gray) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Go
                        ),
                        keyboardActions = KeyboardActions(
                            onGo = {
                                keyboardController?.hide()
                                viewModel.onRequestScan()
                            }
                        ),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.TravelExplore,
                                contentDescription = null,
                                tint = NeonCyan
                            )
                        },
                        trailingIcon = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (state.targetUrl.isNotBlank()) {
                                    IconButton(onClick = { viewModel.onUrlChanged("") }) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Clear URL",
                                            tint = Color.LightGray
                                        )
                                    }
                                } else {
                                    IconButton(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            val clipText = clipboard.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
                                            if (clipText.isNotBlank()) {
                                                viewModel.onUrlChanged(clipText)
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentPaste,
                                            contentDescription = "Paste from clipboard",
                                            tint = NeonCyan
                                        )
                                    }
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = CinemaSurfaceVariant,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = NeonCyan
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("url_input_field")
                    )

                    // Current Scan Level summary indicator
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Layers,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Scan Depth: Level ${state.scanLevels} of 10",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.LightGray
                            )
                        }

                        TextButton(
                            onClick = { viewModel.onRequestScan() },
                            modifier = Modifier.testTag("change_depth_button")
                        ) {
                            Text(
                                text = "Configure Depth",
                                color = NeonCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Scan Action Button
                    val isScanning = state.uiState is ScrapeUiState.Scanning
                    Button(
                        onClick = {
                            keyboardController?.hide()
                            if (isScanning) {
                                viewModel.cancelScan()
                            } else {
                                viewModel.onRequestScan()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isScanning) CoralPink else NeonCyan,
                            contentColor = CinemaBlack
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("scan_button")
                    ) {
                        if (isScanning) {
                            CircularProgressIndicator(
                                color = CinemaBlack,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Cancel Scan",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Radar,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Scan Webpage (${state.scanLevels} Levels)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }
        }

        // Live Scanning Progress / Result Card
        item {
            AnimatedVisibility(
                visible = state.uiState !is ScrapeUiState.Idle,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                when (val uiState = state.uiState) {
                    is ScrapeUiState.Scanning -> {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = CinemaSurface),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        CircularProgressIndicator(
                                            color = NeonCyan,
                                            strokeWidth = 2.5.dp,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = "Level ${uiState.progress.currentLevel} of ${uiState.progress.maxLevel}",
                                            color = NeonCyan,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Surface(
                                        color = VividViolet.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "${uiState.progress.pagesVisited} pages scanned",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = uiState.progress.statusMessage,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )

                                LinearProgressIndicator(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = NeonCyan,
                                    trackColor = CinemaSurfaceVariant
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${uiState.progress.itemsFound} playable media discovered",
                                        color = Color.LightGray,
                                        fontSize = 11.sp
                                    )

                                    TextButton(
                                        onClick = { viewModel.cancelScan() }
                                    ) {
                                        Text("Cancel", color = CoralPink, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    is ScrapeUiState.Success -> {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = CinemaSurface),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Success",
                                        tint = EmeraldGreen,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "Scan Complete!",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                        Text(
                                            text = "Indexed ${uiState.itemsFound} unique media items into Room database.",
                                            color = Color.LightGray,
                                            fontSize = 12.sp
                                        )
                                    }
                                }

                                Button(
                                    onClick = onNavigateToFeed,
                                    colors = ButtonDefaults.buttonColors(containerColor = VividViolet, contentColor = Color.White),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Watch in Reels Feed",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    is ScrapeUiState.Error -> {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = CinemaSurface),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = "Error",
                                    tint = CoralPink,
                                    modifier = Modifier.size(26.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Scrape Notice",
                                        color = CoralPink,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = uiState.message,
                                        color = Color.LightGray,
                                        fontSize = 12.sp
                                    )
                                }
                                IconButton(onClick = { viewModel.resetState() }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Dismiss",
                                        tint = Color.Gray
                                    )
                                }
                            }
                        }
                    }

                    ScrapeUiState.Idle -> Unit
                }
            }
        }

        // Quick Test Sites Section
        item {
            Text(
                text = "Quick Testbeds & Samples",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                text = "Tap any test site to automatically populate the URL and scan media sources:",
                style = MaterialTheme.typography.bodySmall,
                color = Color.LightGray,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }

        items(viewModel.sampleSites) { sample ->
            SampleSiteCard(
                sample = sample,
                onSelect = {
                    viewModel.selectSample(sample)
                    viewModel.onRequestScan()
                }
            )
        }

        // Database Management Footer
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Indexed in local database: ${state.indexedCount} streams",
                    color = Color.Gray,
                    fontSize = 11.sp
                )

                if (state.indexedCount > 0) {
                    TextButton(
                        onClick = { viewModel.clearAllIndexedMedia() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = null,
                            tint = CoralPink,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Clear DB",
                            color = CoralPink,
                            fontSize = 12.sp
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun SampleSiteCard(
    sample: SampleSite,
    onSelect: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CinemaSurface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = sample.title,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    Surface(
                        color = VividViolet.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = sample.tag,
                            color = NeonCyan,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = sample.description,
                    color = Color.LightGray,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = sample.url,
                    color = Color.Gray,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(onClick = onSelect) {
                Icon(
                    imageVector = Icons.Default.Radar,
                    contentDescription = "Scan this sample",
                    tint = NeonCyan
                )
            }
        }
    }
}
