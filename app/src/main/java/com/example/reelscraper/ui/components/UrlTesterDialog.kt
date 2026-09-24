package com.example.reelscraper.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.reelscraper.data.model.ScrapedMedia
import com.example.reelscraper.data.scraper.WebScraperEngine
import com.example.reelscraper.data.settings.AppSettings
import com.example.ui.theme.CinemaCardBg
import com.example.ui.theme.CinemaSurface
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPurple
import kotlinx.coroutines.launch

@Composable
fun UrlTesterDialog(
    scraperEngine: WebScraperEngine,
    onDismiss: () -> Unit,
    onPlayMedia: (ScrapedMedia) -> Unit
) {
    var testUrl by remember { mutableStateOf("") }
    var isTesting by remember { mutableStateOf(false) }
    var diagnosticLog by remember { mutableStateOf<String?>(null) }
    var foundMedia by remember { mutableStateOf<List<ScrapedMedia>>(emptyList()) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CinemaSurface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.BugReport, contentDescription = null, tint = NeonCyan)
                Spacer(modifier = Modifier.width(8.dp))
                Text("URL Stream Diagnostic", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = testUrl,
                    onValueChange = { testUrl = it },
                    label = { Text("Target URL to diagnose") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.LightGray,
                        focusedBorderColor = NeonCyan
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        if (testUrl.isNotBlank()) {
                            isTesting = true
                            diagnosticLog = "Starting diagnostic probe for: $testUrl\n"
                            scope.launch {
                                val startTime = System.currentTimeMillis()
                                try {
                                    val formatted = if (!testUrl.startsWith("http://") && !testUrl.startsWith("https://")) {
                                        "https://$testUrl"
                                    } else testUrl

                                    val candidates = scraperEngine.extractSinglePage(
                                        url = formatted,
                                        settings = AppSettings()
                                    )
                                    val duration = System.currentTimeMillis() - startTime
                                    foundMedia = candidates
                                    diagnosticLog = buildString {
                                        append("✓ Probe Completed in ${duration}ms\n")
                                        append("✓ Candidates discovered: ${candidates.size}\n\n")
                                        candidates.forEachIndexed { i, m ->
                                            append("[$i] ${m.mediaType} (${m.fileExtension})\n")
                                            append("    Title: ${m.title}\n")
                                            append("    URL: ${m.url}\n")
                                            append("    Extractor: ${m.extractorType}\n")
                                        }
                                        if (candidates.isEmpty()) {
                                            append("⚠ No media found with standard rules. Consider enabling WebView fallback or Dynamic Stream Sniffer.\n")
                                        }
                                    }
                                } catch (e: Exception) {
                                    diagnosticLog = "✗ Diagnostic failed: ${e.message}\n"
                                } finally {
                                    isTesting = false
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                    enabled = !isTesting && testUrl.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isTesting) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Probing...")
                    } else {
                        Text("Run Diagnostic Probe", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }

                if (diagnosticLog != null) {
                    Surface(
                        color = CinemaCardBg,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = diagnosticLog ?: "",
                            color = Color.LightGray,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                if (foundMedia.isNotEmpty()) {
                    Text("Extracted Streams:", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    foundMedia.forEach { m ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CinemaCardBg, RoundedCornerShape(6.dp))
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(m.displayTitle, color = Color.White, fontSize = 12.sp, maxLines = 1)
                                Text(m.typeBadge, color = NeonCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { onPlayMedia(m) },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                Text("Play", color = Color.Black, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = NeonCyan)
            }
        }
    )
}
