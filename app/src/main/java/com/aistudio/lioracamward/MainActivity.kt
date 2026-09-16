package com.aistudio.lioracamward

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.aistudio.lioracamward.ui.screens.GuideScreen
import com.aistudio.lioracamward.ui.screens.HistoryScreen
import com.aistudio.lioracamward.ui.screens.HomeScreen
import com.aistudio.lioracamward.ui.screens.ScanDetailScreen
import com.aistudio.lioracamward.ui.screens.ScanScreen
import com.aistudio.lioracamward.ui.theme.CyberCard
import com.aistudio.lioracamward.ui.theme.CyberCardBorder
import com.aistudio.lioracamward.ui.theme.LioraTheme
import com.aistudio.lioracamward.ui.theme.RadarCyan
import com.aistudio.lioracamward.ui.theme.RadarNeonGreen
import com.aistudio.lioracamward.ui.theme.TextMuted
import com.aistudio.lioracamward.ui.theme.TextPrimary

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as LioraApp
        val repository = app.scanRepository

        setContent {
            LioraTheme {
                MainAppNavHost(repository = repository)
            }
        }
    }
}

sealed class Screen(val route: String, val title: String) {
    object Home : Screen("home", "Radar")
    object Scan : Screen("scan", "Inspect")
    object History : Screen("history", "History")
    object Guide : Screen("guide", "Guide")
    object Detail : Screen("detail/{scanId}", "Report") {
        fun createRoute(scanId: String) = "detail/$scanId"
    }
}

@Composable
fun MainAppNavHost(repository: com.aistudio.lioracamward.data.repository.ScanRepository) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination?.route

    val bottomNavItems = listOf(
        Screen.Home to Icons.Default.Radar,
        Screen.Scan to Icons.Default.Search,
        Screen.History to Icons.Default.History,
        Screen.Guide to Icons.Default.Info
    )

    val showBottomBar = currentDestination in listOf(
        Screen.Home.route,
        Screen.History.route,
        Screen.Guide.route
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = CyberCard,
                    tonalElevation = 8.dp
                ) {
                    bottomNavItems.forEach { (screen, icon) ->
                        val isSelected = currentDestination == screen.route
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = screen.title
                                )
                            },
                            label = {
                                Text(
                                    text = screen.title,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFF041A10),
                                selectedTextColor = RadarNeonGreen,
                                indicatorColor = RadarNeonGreen,
                                unselectedIconColor = TextMuted,
                                unselectedTextColor = TextMuted
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route
            ) {
                composable(Screen.Home.route) {
                    HomeScreen(
                        onStartScan = { navController.navigate(Screen.Scan.route) },
                        onViewHistory = { navController.navigate(Screen.History.route) },
                        onViewGuide = { navController.navigate(Screen.Guide.route) }
                    )
                }

                composable(Screen.Scan.route) {
                    ScanScreen(
                        scanRepository = repository,
                        onFinishScan = { scanId ->
                            navController.navigate(Screen.Detail.createRoute(scanId)) {
                                popUpTo(Screen.Home.route)
                            }
                        },
                        onCancel = { navController.popBackStack() }
                    )
                }

                composable(Screen.History.route) {
                    HistoryScreen(
                        scanRepository = repository,
                        onSelectScan = { scanId ->
                            navController.navigate(Screen.Detail.createRoute(scanId))
                        },
                        onStartNewScan = { navController.navigate(Screen.Scan.route) }
                    )
                }

                composable(Screen.Guide.route) {
                    GuideScreen(
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(
                    route = Screen.Detail.route,
                    arguments = listOf(navArgument("scanId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val scanId = backStackEntry.arguments?.getString("scanId") ?: ""
                    ScanDetailScreen(
                        scanId = scanId,
                        scanRepository = repository,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
