package com.example.reelscraper.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Reels : Screen("reels", "Reels", Icons.Default.PlayCircle)
    object Scraper : Screen("scraper", "Scraper", Icons.Default.Radar)
    object Library : Screen("library", "Library", Icons.Default.VideoLibrary)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}
