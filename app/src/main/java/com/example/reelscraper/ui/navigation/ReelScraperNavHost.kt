package com.example.reelscraper.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.reelscraper.di.AppContainer
import com.example.reelscraper.ui.screens.ReelsFeedScreen
import com.example.reelscraper.ui.screens.ScraperScreen
import com.example.reelscraper.ui.screens.SearchScreen
import com.example.reelscraper.ui.screens.SettingsScreen
import com.example.reelscraper.ui.viewmodel.FeedViewModel
import com.example.reelscraper.ui.viewmodel.ScraperViewModel
import com.example.reelscraper.ui.viewmodel.SearchViewModel
import com.example.reelscraper.ui.viewmodel.SettingsViewModel
import com.example.ui.theme.CinemaBlack
import com.example.ui.theme.CinemaSurface
import com.example.ui.theme.NeonCyan

@Composable
fun ReelScraperNavHost(
    appContainer: AppContainer,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()

    val scraperViewModel: ScraperViewModel = viewModel(factory = appContainer.viewModelFactory)
    val feedViewModel: FeedViewModel = viewModel(factory = appContainer.viewModelFactory)
    val searchViewModel: SearchViewModel = viewModel(factory = appContainer.viewModelFactory)
    val settingsViewModel: SettingsViewModel = viewModel(factory = appContainer.viewModelFactory)

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Reels.route

    val navItems = listOf(
        Screen.Reels,
        Screen.Scraper,
        Screen.Library,
        Screen.Settings
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = CinemaBlack,
        bottomBar = {
            NavigationBar(
                containerColor = CinemaSurface.copy(alpha = 0.95f),
                tonalElevation = 8.dp
            ) {
                navItems.forEach { screen ->
                    val isSelected = currentRoute == screen.route
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            if (currentRoute != screen.route) {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = screen.title,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = {
                            Text(
                                text = screen.title,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NeonCyan,
                            selectedTextColor = NeonCyan,
                            unselectedIconColor = Color.Gray,
                            unselectedTextColor = Color.Gray,
                            indicatorColor = CinemaSurface
                        ),
                        modifier = Modifier.testTag("nav_item_${screen.route}")
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Reels.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Reels.route) {
                ReelsFeedScreen(
                    viewModel = feedViewModel,
                    onNavigateToScraper = {
                        navController.navigate(Screen.Scraper.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(Screen.Scraper.route) {
                ScraperScreen(
                    viewModel = scraperViewModel,
                    onNavigateToFeed = {
                        navController.navigate(Screen.Reels.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(Screen.Library.route) {
                val mediaList by feedViewModel.mediaList.collectAsStateWithLifecycle()
                SearchScreen(
                    viewModel = searchViewModel,
                    onPlayMedia = { media ->
                        val index = mediaList.indexOfFirst { it.id == media.id }
                        if (index != -1) {
                            feedViewModel.jumpToIndex(index)
                        }
                        navController.navigate(Screen.Reels.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    viewModel = settingsViewModel
                )
            }
        }
    }
}
