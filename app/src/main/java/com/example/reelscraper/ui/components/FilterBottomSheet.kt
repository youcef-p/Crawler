package com.example.reelscraper.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FilterListOff
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stream
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CinemaBlack
import com.example.ui.theme.CinemaSurface
import com.example.ui.theme.CinemaSurfaceVariant
import com.example.ui.theme.CoralPink
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.VividViolet

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FilterBottomSheet(
    keyword: String,
    onKeywordChanged: (String) -> Unit,
    availableDomains: List<String>,
    selectedDomains: Set<String>,
    onToggleDomain: (String) -> Unit,
    onSelectAllDomains: () -> Unit,
    selectedFormat: String? = "all",
    onFormatSelected: (String) -> Unit = {},
    onlyFavorites: Boolean = false,
    onToggleFavorites: () -> Unit = {},
    onlyDynamic: Boolean = false,
    onToggleDynamic: () -> Unit = {},
    hideBroken: Boolean = true,
    onToggleHideBroken: () -> Unit = {},
    onClearFilters: () -> Unit,
    matchCount: Int,
    onDismiss: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CinemaSurface,
        dragHandle = {
            Surface(
                color = CinemaSurfaceVariant,
                shape = CircleShape,
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .size(width = 36.dp, height = 4.dp)
            ) {}
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: Title & Clear Filters button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Filter Media Feed",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                TextButton(
                    onClick = onClearFilters,
                    colors = ButtonDefaults.textButtonColors(contentColor = CoralPink),
                    modifier = Modifier.testTag("clear_filters_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterListOff,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear Filters", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Keyword Filter Field with Search Operators explanation
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Keyword Search (supports source:, format:, dynamic:true)",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray,
                    fontWeight = FontWeight.Medium
                )

                OutlinedTextField(
                    value = keyword,
                    onValueChange = onKeywordChanged,
                    placeholder = { Text("Search title, URL, or format...", color = Color.Gray, fontSize = 13.sp) },
                    singleLine = true,
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = NeonCyan)
                    },
                    trailingIcon = {
                        if (keyword.isNotBlank()) {
                            IconButton(onClick = { onKeywordChanged("") }) {
                                Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear", tint = Color.LightGray)
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
                        .testTag("filter_keyword_input")
                )
            }

            // Quick Filter Toggles (Favorites, Dynamic Streams, Broken)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Quick Filters",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray,
                    fontWeight = FontWeight.Medium
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = onlyFavorites,
                        onClick = onToggleFavorites,
                        leadingIcon = { Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        label = { Text("Favorites Only", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CoralPink,
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White
                        )
                    )

                    FilterChip(
                        selected = onlyDynamic,
                        onClick = onToggleDynamic,
                        leadingIcon = { Icon(Icons.Default.Stream, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        label = { Text("Dynamic Streams Only", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NeonPurple,
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White
                        )
                    )

                    FilterChip(
                        selected = hideBroken,
                        onClick = onToggleHideBroken,
                        leadingIcon = { Icon(Icons.Default.VisibilityOff, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        label = { Text("Hide Broken", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color.DarkGray,
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White
                        )
                    )
                }
            }

            // Media Format Chips
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Media Format",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray,
                    fontWeight = FontWeight.Medium
                )

                val formats = listOf("all", "mp4", "webm", "hls", "dash", "gif")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    formats.forEach { fmt ->
                        val isSelected = (selectedFormat ?: "all").equals(fmt, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = { onFormatSelected(fmt) },
                            label = { Text(fmt.uppercase(), fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeonCyan,
                                selectedLabelColor = CinemaBlack,
                                containerColor = CinemaBlack.copy(alpha = 0.5f),
                                labelColor = Color.LightGray
                            )
                        )
                    }
                }
            }

            // Source Website / Domain Chips
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Language, contentDescription = null, tint = VividViolet, modifier = Modifier.size(16.dp))
                        Text(
                            text = "Source Websites (${availableDomains.size})",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.LightGray,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Text(
                        text = if (selectedDomains.isEmpty()) "All Selected" else "${selectedDomains.size} Selected",
                        fontSize = 11.sp,
                        color = NeonCyan,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Default "All Sources" Chip
                    FilterChip(
                        selected = selectedDomains.isEmpty(),
                        onClick = onSelectAllDomains,
                        label = { Text("All Sources", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = VividViolet,
                            selectedLabelColor = Color.White,
                            labelColor = Color.LightGray
                        ),
                        modifier = Modifier.testTag("source_chip_all")
                    )

                    // Individual Domain Chips
                    availableDomains.forEach { domain ->
                        val isSelected = selectedDomains.contains(domain)
                        FilterChip(
                            selected = isSelected,
                            onClick = { onToggleDomain(domain) },
                            label = { Text(domain, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeonCyan,
                                selectedLabelColor = CinemaBlack,
                                containerColor = CinemaBlack.copy(alpha = 0.5f),
                                labelColor = Color.LightGray
                            ),
                            modifier = Modifier.testTag("source_chip_$domain")
                        )
                    }
                }
            }

            // Result Count and Apply Button
            Surface(
                color = CinemaBlack.copy(alpha = 0.6f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "$matchCount matching items",
                            color = NeonCyan,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (keyword.isNotBlank() || selectedDomains.isNotEmpty() || (selectedFormat != null && selectedFormat != "all")) "Filtered feed active" else "Showing full library",
                            color = Color.Gray,
                            fontSize = 11.sp
                        )
                    }

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = CinemaBlack),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("apply_filters_button")
                    ) {
                        Text("Show Results", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
