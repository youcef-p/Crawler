package com.example.reelscraper.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CinemaBlack
import com.example.ui.theme.CinemaSurface
import com.example.ui.theme.CinemaSurfaceVariant
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.VividViolet

@Composable
fun CrawlDepthDialog(
    initialLevels: Int = 2,
    targetUrl: String,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedLevels by remember { mutableIntStateOf(initialLevels.coerceIn(1, 10)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CinemaSurface,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = VividViolet.copy(alpha = 0.2f),
                    shape = CircleShape,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Text(
                    text = "How many levels should the app scan?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Target URL: $targetUrl",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray,
                    maxLines = 2
                )

                // Large Level Indicator with stepper
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledIconButton(
                        onClick = { if (selectedLevels > 1) selectedLevels-- },
                        enabled = selectedLevels > 1,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = CinemaSurfaceVariant,
                            contentColor = Color.White
                        ),
                        modifier = Modifier.testTag("stepper_decrement")
                    ) {
                        Icon(imageVector = Icons.Default.Remove, contentDescription = "Decrease Level")
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    Surface(
                        color = VividViolet.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = "Level $selectedLevels",
                                color = NeonCyan,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = if (selectedLevels == 1) "Single Page" else "$selectedLevels Degrees of Links",
                                color = Color.LightGray,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    FilledIconButton(
                        onClick = { if (selectedLevels < 10) selectedLevels++ },
                        enabled = selectedLevels < 10,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = CinemaSurfaceVariant,
                            contentColor = Color.White
                        ),
                        modifier = Modifier.testTag("stepper_increment")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Increase Level")
                    }
                }

                // Slider (1 to 10)
                Column {
                    Slider(
                        value = selectedLevels.toFloat(),
                        onValueChange = { selectedLevels = it.toInt().coerceIn(1, 10) },
                        valueRange = 1f..10f,
                        steps = 8,
                        colors = SliderDefaults.colors(
                            thumbColor = NeonCyan,
                            activeTrackColor = NeonCyan,
                            inactiveTrackColor = CinemaSurfaceVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("level_slider")
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("1 (Page only)", fontSize = 10.sp, color = Color.Gray)
                        Text("5 (Deep)", fontSize = 10.sp, color = Color.Gray)
                        Text("10 (Max)", fontSize = 10.sp, color = Color.Gray)
                    }
                }

                // Description of what the selected level does
                Surface(
                    color = CinemaBlack.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = when (selectedLevels) {
                            1 -> "• Level 1: Scans the initial URL only and extracts all playable media from it."
                            2 -> "• Level 1: Scans the initial URL.\n• Level 2: Scans all clickable links found on the initial URL (Default)."
                            3 -> "• Levels 1 & 2: Scans seed URL + Level 2 links.\n• Level 3: Follows clickable links found on Level 2 pages."
                            else -> "• Level 1 to $selectedLevels: Breadth-first crawl following links recursively through $selectedLevels scan levels."
                        },
                        color = Color.LightGray,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }

                // Quick Presets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedLevels == 1,
                        onClick = { selectedLevels = 1 },
                        label = { Text("Level 1 (Fast)", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = VividViolet,
                            selectedLabelColor = Color.White,
                            labelColor = Color.LightGray
                        )
                    )
                    FilterChip(
                        selected = selectedLevels == 2,
                        onClick = { selectedLevels = 2 },
                        label = { Text("Level 2 (Default)", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = VividViolet,
                            selectedLabelColor = Color.White,
                            labelColor = Color.LightGray
                        )
                    )
                    FilterChip(
                        selected = selectedLevels == 3,
                        onClick = { selectedLevels = 3 },
                        label = { Text("Level 3 (Deep)", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = VividViolet,
                            selectedLabelColor = Color.White,
                            labelColor = Color.LightGray
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedLevels) },
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = CinemaBlack),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("confirm_scan_button")
            ) {
                Text("Start Scan", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = Color.LightGray),
                modifier = Modifier.testTag("cancel_scan_dialog_button")
            ) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}
