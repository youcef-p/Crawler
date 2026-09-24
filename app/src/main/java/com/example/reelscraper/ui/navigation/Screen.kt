package com.example.reelscraper.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Difference
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Reels : Screen("reels", "Reels", Icons.Default.PlayCircle)
    object Scraper : Screen("scraper", "Scraper", Icons.Default.Radar)
    object Sniffer : Screen("sniffer", "Sniffer", Icons.Default.Sensors)
    object Library : Screen("library", "Library", Icons.Default.VideoLibrary)
    object SiteProfiles : Screen("site_profiles", "Profiles", Icons.Default.Language)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    object Duplicates : Screen("duplicates", "Duplicates", Icons.Default.Difference)
}
