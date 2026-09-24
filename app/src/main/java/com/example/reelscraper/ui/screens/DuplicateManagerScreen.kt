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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Difference
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.reelscraper.data.model.ScrapedMedia
import com.example.reelscraper.data.repository.MediaRepository
import com.example.reelscraper.intelligence.phash.PerceptualHashGenerator
import com.example.ui.theme.CinemaBlack
import com.example.ui.theme.CinemaSurface
import com.example.ui.theme.CoralPink
import com.example.ui.theme.NeonCyan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class DuplicateGroup(
    val primary: ScrapedMedia,
    val duplicate: ScrapedMedia,
    val distance: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuplicateManagerScreen(
    mediaRepository: MediaRepository,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isScanning by remember { mutableStateOf(false) }
    var scanProgressMessage by remember { mutableStateOf<String?>(null) }
    var duplicateGroups by remember { mutableStateOf<List<DuplicateGroup>>(emptyList()) }

    fun scanDuplicates() {
        coroutineScope.launch {
            isScanning = true
            scanProgressMessage = "Loading media library..."
            val allMedia: List<ScrapedMedia> = withContext(Dispatchers.IO) {
                mediaRepository.getAllMediaDirect()
            }

            scanProgressMessage = "Computing visual perceptual hashes (pHash)..."
            val updatedMedia = mutableListOf<ScrapedMedia>()

            for (idx in allMedia.indices) {
                val media = allMedia[idx]
                scanProgressMessage = "Analyzing frame ${idx + 1}/${allMedia.size}: ${media.displayTitle.take(20)}"
                var currentHash = media.pHash
                if (currentHash.isNullOrBlank()) {
                    currentHash = PerceptualHashGenerator.generateHash(context, media)
                    if (currentHash != null) {
                        val updated = media.copy(pHash = currentHash)
                        withContext(Dispatchers.IO) {
                            mediaRepository.updateMedia(updated)
                        }
                        updatedMedia.add(updated)
                    } else {
                        updatedMedia.add(media)
                    }
                } else {
                    updatedMedia.add(media)
                }
            }

            scanProgressMessage = "Matching perceptual similarities..."
            val groups = mutableListOf<DuplicateGroup>()
            val processed = mutableSetOf<Long>()

            for (i in updatedMedia.indices) {
                val m1 = updatedMedia[i]
                val hash1 = m1.pHash
                if (hash1.isNullOrBlank() || processed.contains(m1.id)) continue

                for (j in i + 1 until updatedMedia.size) {
                    val m2 = updatedMedia[j]
                    val hash2 = m2.pHash
                    if (hash2.isNullOrBlank() || processed.contains(m2.id)) continue

                    val distance = PerceptualHashGenerator.hammingDistance(hash1, hash2)
                    if (distance <= 10) {
                        groups.add(DuplicateGroup(primary = m1, duplicate = m2, distance = distance))
                        processed.add(m2.id)
                    }
                }
            }

            duplicateGroups = groups
            isScanning = false
            scanProgressMessage = null
        }
    }

    LaunchedEffect(Unit) {
        scanDuplicates()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Visual Duplicate Manager",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { scanDuplicates() },
                        enabled = !isScanning,
                        modifier = Modifier.testTag("rescan_duplicates_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoFixHigh,
                            contentDescription = "Rescan",
                            tint = NeonCyan
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CinemaBlack)
            )
        },
        containerColor = CinemaBlack,
        modifier = modifier.fillMaxSize().testTag("duplicate_manager_screen")
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isScanning) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(color = NeonCyan)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = scanProgressMessage ?: "Analyzing visual hashes...",
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
            } else if (duplicateGroups.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No Perceptual Duplicates Found",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "All indexed streams and videos have distinct visual hashes.",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text(
                            text = "Found ${duplicateGroups.size} visually similar duplicate pairs (Hamming distance ≤ 10):",
                            color = Color.LightGray,
                            fontSize = 13.sp
                        )
                    }

                    items(duplicateGroups) { group ->
                        DuplicateGroupCard(
                            group = group,
                            onDelete = { mediaId ->
                                coroutineScope.launch {
                                    withContext(Dispatchers.IO) {
                                        mediaRepository.deleteMedia(mediaId)
                                    }
                                    duplicateGroups = duplicateGroups.filter {
                                        it.primary.id != mediaId && it.duplicate.id != mediaId
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DuplicateGroupCard(
    group: DuplicateGroup,
    onDelete: (Long) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CinemaSurface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().testTag("duplicate_pair_card")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Similarity: ${((64 - group.distance) * 100) / 64}% match",
                    color = NeonCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "pHash distance: ${group.distance}",
                    color = Color.Gray,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Primary
                Column(modifier = Modifier.weight(1f)) {
                    AsyncImage(
                        model = group.primary.thumbnailUrl,
                        contentDescription = group.primary.displayTitle,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(90.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = group.primary.displayTitle,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${group.primary.typeBadge} • ${group.primary.sourceDomain}",
                        color = Color.LightGray,
                        fontSize = 10.sp
                    )
                }

                // Duplicate
                Column(modifier = Modifier.weight(1f)) {
                    AsyncImage(
                        model = group.duplicate.thumbnailUrl,
                        contentDescription = group.duplicate.displayTitle,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(90.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = group.duplicate.displayTitle,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${group.duplicate.typeBadge} • ${group.duplicate.sourceDomain}",
                        color = Color.LightGray,
                        fontSize = 10.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Button(
                        onClick = { onDelete(group.duplicate.id) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CoralPink.copy(alpha = 0.2f),
                            contentColor = CoralPink
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete Dupe", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
