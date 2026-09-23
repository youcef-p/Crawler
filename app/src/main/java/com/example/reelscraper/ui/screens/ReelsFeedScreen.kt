package com.example.reelscraper.ui.screens

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
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FilterListOff
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.reelscraper.ui.components.CustomVideoPlayer
import com.example.reelscraper.ui.components.FilterBottomSheet
import com.example.reelscraper.ui.components.ReelOverlayControls
import com.example.reelscraper.ui.viewmodel.FeedViewModel
import com.example.ui.theme.CinemaBlack
import com.example.ui.theme.CinemaSurface
import com.example.ui.theme.CoralPink
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.VividViolet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReelsFeedScreen(
    viewModel: FeedViewModel,
    onNavigateToScraper: () -> Unit,
    modifier: Modifier = Modifier
) {
    val mediaList by viewModel.mediaList.collectAsStateWithLifecycle()
    val feedState by viewModel.feedState.collectAsStateWithLifecycle()
    val keyword by viewModel.keyword.collectAsStateWithLifecycle()
    val selectedDomains by viewModel.selectedDomains.collectAsStateWithLifecycle()
    val availableDomains by viewModel.availableDomains.collectAsStateWithLifecycle()
    val hasActiveFilters by viewModel.hasActiveFilters.collectAsStateWithLifecycle()

    if (feedState.isFilterSheetVisible) {
        FilterBottomSheet(
            keyword = keyword,
            onKeywordChanged = { viewModel.onKeywordChanged(it) },
            availableDomains = availableDomains,
            selectedDomains = selectedDomains,
            onToggleDomain = { viewModel.toggleDomainSelection(it) },
            onSelectAllDomains = { viewModel.selectAllDomains() },
            onClearFilters = { viewModel.clearAllFilters() },
            matchCount = mediaList.size,
            onDismiss = { viewModel.closeFilterSheet() }
        )
    }

    if (mediaList.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(CinemaBlack)
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    color = VividViolet.copy(alpha = 0.15f),
                    shape = CircleShape,
                    modifier = Modifier.size(96.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (hasActiveFilters) Icons.Default.FilterListOff else Icons.Default.Videocam,
                            contentDescription = "No Media",
                            tint = if (hasActiveFilters) CoralPink else NeonCyan,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = if (hasActiveFilters) "No Matching Media Found" else "No Streams Indexed Yet",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (hasActiveFilters)
                        "No videos or streams matched your active keyword ('$keyword') or selected domain filter."
                    else
                        "Scan any webpage URL to extract and play video streams, HLS feeds, and GIFs in full-screen reels.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.LightGray,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(28.dp))

                if (hasActiveFilters) {
                    Button(
                        onClick = { viewModel.clearAllFilters() },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = CinemaBlack),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("reset_filters_button")
                    ) {
                        Icon(imageVector = Icons.Default.FilterListOff, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(text = "Reset All Filters", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = onNavigateToScraper,
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = CinemaBlack),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("scan_now_button")
                    ) {
                        Icon(imageVector = Icons.Default.TravelExplore, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(text = "Scan a Webpage", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        return
    }

    val pagerState = rememberPagerState(
        initialPage = feedState.currentItemIndex.coerceIn(0, mediaList.lastIndex),
        pageCount = { mediaList.size }
    )

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { pageIndex ->
            viewModel.onPageChanged(pageIndex)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CinemaBlack)
    ) {
        VerticalPager(
            state = pagerState,
            beyondViewportPageCount = 1,
            key = { index -> mediaList[index].url },
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val mediaItem = mediaList[page]
            val isActive = page == pagerState.currentPage

            Box(modifier = Modifier.fillMaxSize()) {
                CustomVideoPlayer(
                    media = mediaItem,
                    isActive = isActive,
                    isPlaying = feedState.isPlaying,
                    isMuted = feedState.isMuted,
                    onProgressUpdate = { pos, dur, buffering ->
                        if (isActive) {
                            viewModel.updateProgress(pos, dur, buffering)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                ReelOverlayControls(
                    media = mediaItem,
                    isPlaying = feedState.isPlaying,
                    isMuted = feedState.isMuted,
                    currentPositionMs = feedState.currentPositionMs,
                    totalDurationMs = feedState.totalDurationMs,
                    onTogglePlayPause = { viewModel.togglePlayPause() },
                    onToggleMute = { viewModel.toggleMute() },
                    onToggleFavorite = { viewModel.toggleFavorite(mediaItem) },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Top Status Bar: Pill badge & Filter Action Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.55f),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(
                    text = "ReelScraper  •  ${pagerState.currentPage + 1}/${mediaList.size}",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            // Filter button with badge if active filters exist
            Surface(
                color = if (hasActiveFilters) VividViolet.copy(alpha = 0.85f) else Color.Black.copy(alpha = 0.55f),
                shape = CircleShape,
                modifier = Modifier.size(40.dp)
            ) {
                IconButton(
                    onClick = { viewModel.openFilterSheet() },
                    modifier = Modifier.testTag("feed_filter_button")
                ) {
                    BadgedBox(
                        badge = {
                            if (hasActiveFilters) {
                                Badge(
                                    containerColor = NeonCyan,
                                    contentColor = CinemaBlack
                                ) {
                                    val filterCount = (if (keyword.isNotBlank()) 1 else 0) + selectedDomains.size
                                    Text(text = filterCount.toString())
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Filter playback feed",
                            tint = if (hasActiveFilters) NeonCyan else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
