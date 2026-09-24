package com.example.reelscraper.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.reelscraper.data.model.Chapter
import com.example.reelscraper.data.model.SubtitleTrack
import com.example.ui.theme.CinemaBlack
import com.example.ui.theme.CinemaSurface
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.VividViolet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerTrackMenuSheet(
    chapters: List<Chapter>,
    subtitles: List<SubtitleTrack>,
    availableQualities: List<String> = listOf("Auto", "Highest", "1080p", "720p", "480p", "Lowest", "Data Saver"),
    selectedQuality: String,
    selectedSubtitleId: Long?,
    onSelectQuality: (String) -> Unit,
    onSelectSubtitle: (SubtitleTrack?) -> Unit,
    onSeekToChapter: (Long) -> Unit,
    onAddChapter: (String, Long) -> Unit,
    currentPositionMs: Long,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedTab by remember { mutableIntStateOf(0) }
    var newChapterTitle by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CinemaSurface,
        modifier = Modifier.testTag("player_track_menu_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Stream Controls & Tracks",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = CinemaSurface,
                contentColor = NeonCyan,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = NeonCyan
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Quality") },
                    icon = { Icon(Icons.Default.HighQuality, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Chapters (${chapters.size})") },
                    icon = { Icon(Icons.Default.List, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Subtitles") },
                    icon = { Icon(Icons.Default.ClosedCaption, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (selectedTab) {
                0 -> {
                    // Quality Selection
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(availableQualities) { quality ->
                            val isSelected = quality.equals(selectedQuality, ignoreCase = true)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSelectQuality(quality)
                                        onDismiss()
                                    }
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = quality,
                                    color = if (isSelected) NeonCyan else Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = "Selected", tint = NeonCyan)
                                }
                            }
                            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                        }
                    }
                }
                1 -> {
                    // Chapters List & Manual Add
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newChapterTitle,
                            onValueChange = { newChapterTitle = it },
                            placeholder = { Text("Chapter Title...", color = Color.Gray) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                unfocusedBorderColor = Color.DarkGray,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                if (newChapterTitle.isNotBlank()) {
                                    onAddChapter(newChapterTitle, currentPositionMs)
                                    newChapterTitle = ""
                                }
                            },
                            modifier = Modifier.testTag("add_chapter_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Chapter", tint = NeonCyan)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (chapters.isEmpty()) {
                        Text(
                            text = "No chapters detected for this video.",
                            color = Color.Gray,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxWidth().height(240.dp)) {
                            items(chapters) { chapter ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onSeekToChapter(chapter.positionMillis)
                                            onDismiss()
                                        }
                                        .padding(vertical = 10.dp, horizontal = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = chapter.title,
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = formatDuration(chapter.positionMillis),
                                            color = NeonCyan,
                                            fontSize = 12.sp
                                        )
                                    }
                                    Text(
                                        text = chapter.source.name,
                                        color = Color.Gray,
                                        fontSize = 10.sp
                                    )
                                }
                                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                            }
                        }
                    }
                }
                2 -> {
                    // Subtitles Selection
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSelectSubtitle(null)
                                        onDismiss()
                                    }
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "None (Subtitles Off)",
                                    color = if (selectedSubtitleId == null) NeonCyan else Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = if (selectedSubtitleId == null) FontWeight.Bold else FontWeight.Normal
                                )
                                if (selectedSubtitleId == null) {
                                    Icon(Icons.Default.Check, contentDescription = "Selected", tint = NeonCyan)
                                }
                            }
                            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                        }

                        items(subtitles) { track ->
                            val isSelected = track.id == selectedSubtitleId
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSelectSubtitle(track)
                                        onDismiss()
                                    }
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = track.label,
                                        color = if (isSelected) NeonCyan else Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                    Text(
                                        text = "${track.language} • ${track.type}",
                                        color = Color.LightGray,
                                        fontSize = 11.sp
                                    )
                                }
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = "Selected", tint = NeonCyan)
                                }
                            }
                            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                        }
                    }
                }
            }
        }
    }
}

private fun formatDuration(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
