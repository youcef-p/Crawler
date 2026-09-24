@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class, androidx.media3.common.util.UnstableApi::class)

package com.example.reelscraper.ui.screens

import android.view.ViewGroup
import android.widget.FrameLayout
import kotlin.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.reelscraper.data.model.MediaType
import com.example.reelscraper.data.model.ScrapedMedia
import com.example.reelscraper.player.SharedPreviewPlayer
import com.example.reelscraper.ui.viewmodel.SearchViewModel
import com.example.ui.theme.CinemaBlack
import com.example.ui.theme.CinemaSurface
import com.example.ui.theme.CinemaSurfaceVariant
import com.example.ui.theme.CoralPink
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.VividViolet

@OptIn(UnstableApi::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    sharedPreviewPlayer: SharedPreviewPlayer? = null,
    onPlayMedia: (ScrapedMedia) -> Unit,
    modifier: Modifier = Modifier
) {
    val screenState by viewModel.screenState.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val totalCount by viewModel.totalCount.collectAsStateWithLifecycle()
    val keyboardController = LocalSoftwareKeyboardController.current

    var activePreviewMediaId by remember { mutableStateOf<Long?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            sharedPreviewPlayer?.stopPreview()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CinemaBlack)
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Indexed Media Library",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "$totalCount total items in database",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
            value = screenState.searchQuery,
            onValueChange = { viewModel.onQueryChanged(it) },
            placeholder = { Text("Filter indexed media by title or URL...", color = Color.Gray, fontSize = 14.sp) },
            singleLine = true,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = NeonCyan
                )
            },
            trailingIcon = {
                if (screenState.searchQuery.isNotBlank()) {
                    IconButton(onClick = { viewModel.clearQuery() }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear",
                            tint = Color.LightGray
                        )
                    }
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() }),
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
                .testTag("search_text_field")
        )

        Spacer(modifier = Modifier.height(10.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                FilterChip(
                    selected = screenState.selectedFilter == null && !screenState.showOnlyFavorites,
                    onClick = { viewModel.onFilterSelected(null) },
                    label = { Text("All (${searchResults.size})", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NeonCyan,
                        selectedLabelColor = CinemaBlack,
                        labelColor = Color.LightGray
                    )
                )
            }
            item {
                FilterChip(
                    selected = screenState.selectedFilter == MediaType.VIDEO,
                    onClick = { viewModel.onFilterSelected(MediaType.VIDEO) },
                    label = { Text("Videos (MP4/WebM)", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = VividViolet,
                        selectedLabelColor = Color.White,
                        labelColor = Color.LightGray
                    )
                )
            }
            item {
                FilterChip(
                    selected = screenState.selectedFilter == MediaType.HLS,
                    onClick = { viewModel.onFilterSelected(MediaType.HLS) },
                    label = { Text("HLS / M3U8", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = VividViolet,
                        selectedLabelColor = Color.White,
                        labelColor = Color.LightGray
                    )
                )
            }
            item {
                FilterChip(
                    selected = screenState.selectedFilter == MediaType.DASH,
                    onClick = { viewModel.onFilterSelected(MediaType.DASH) },
                    label = { Text("DASH", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = VividViolet,
                        selectedLabelColor = Color.White,
                        labelColor = Color.LightGray
                    )
                )
            }
            item {
                FilterChip(
                    selected = screenState.selectedFilter == MediaType.GIF,
                    onClick = { viewModel.onFilterSelected(MediaType.GIF) },
                    label = { Text("GIFs", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = VividViolet,
                        selectedLabelColor = Color.White,
                        labelColor = Color.LightGray
                    )
                )
            }
            item {
                FilterChip(
                    selected = screenState.showOnlyFavorites,
                    onClick = { viewModel.onFavoritesToggled(!screenState.showOnlyFavorites) },
                    label = { Text("Favorites", fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CoralPink,
                        selectedLabelColor = Color.White,
                        labelColor = Color.LightGray
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (searchResults.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (screenState.searchQuery.isNotBlank()) "No media matching '${screenState.searchQuery}'" else "No media found in library",
                        color = Color.LightGray,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(searchResults, key = { _, item -> item.id }) { _, media ->
                    MediaIndexCard(
                        media = media,
                        isPreviewActive = activePreviewMediaId == media.id,
                        sharedPreviewPlayer = sharedPreviewPlayer,
                        onPlay = { onPlayMedia(media) },
                        onStartHoverPreview = {
                            activePreviewMediaId = media.id
                            sharedPreviewPlayer?.startPreview(media)
                        },
                        onStopHoverPreview = {
                            if (activePreviewMediaId == media.id) {
                                activePreviewMediaId = null
                                sharedPreviewPlayer?.stopPreview()
                            }
                        },
                        onToggleFavorite = { viewModel.toggleFavorite(media) },
                        onDelete = { viewModel.deleteMedia(media.id) }
                    )
                }
            }
        }
    }
}

@OptIn(UnstableApi::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun MediaIndexCard(
    media: ScrapedMedia,
    isPreviewActive: Boolean,
    sharedPreviewPlayer: SharedPreviewPlayer?,
    onPlay: () -> Unit,
    onStartHoverPreview: () -> Unit,
    onStopHoverPreview: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current

    Card(
        colors = CardDefaults.cardColors(containerColor = CinemaSurface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onPlay,
                onLongClick = {
                    onStartHoverPreview()
                }
            )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail / Live Preview Box
            Box(
                modifier = Modifier
                    .size(width = 90.dp, height = 75.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(CinemaSurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (isPreviewActive && sharedPreviewPlayer != null) {
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                useController = false
                                player = sharedPreviewPlayer.player
                                layoutParams = FrameLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    if (!media.thumbnailUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(media.thumbnailUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = media.displayTitle,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.PlayCircle,
                        contentDescription = "Play",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Metadata column
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = when (media.mediaType) {
                            MediaType.HLS -> VividViolet
                            MediaType.DASH -> CoralPink
                            MediaType.GIF -> NeonCyan
                            MediaType.VIDEO -> CinemaSurfaceVariant
                        },
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = media.typeBadge,
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (media.sourceDomain.isNotBlank()) {
                        Text(
                            text = media.sourceDomain,
                            color = Color.LightGray,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = media.displayTitle,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = media.url,
                    color = Color.Gray,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Action Icons
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (media.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (media.isFavorite) CoralPink else Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
