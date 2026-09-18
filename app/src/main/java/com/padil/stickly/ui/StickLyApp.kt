package com.padil.stickly.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.padil.stickly.ui.editor.EditorScreen
import com.padil.stickly.ui.home.HomeScreen
import com.padil.stickly.ui.info.InfoScreen
import com.padil.stickly.ui.pack.PackOverviewScreen

object StickLyRoutes {
    const val HOME = "home"
    const val EDITOR = "editor?packId={packId}"
    const val EDITOR_REF = "editor?packId="
    const val PACK = "pack/{packId}"
    const val INFO = "info"
}

@Composable
fun StickLyApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            if (currentRoute != StickLyRoutes.PACK) {
                AppBottomBar(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onCreateFresh = {
                        navController.navigate(StickLyRoutes.EDITOR_REF) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = false
                        }
                    },
                )
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = StickLyRoutes.HOME,
            modifier = Modifier.padding(padding),
        ) {
            composable(StickLyRoutes.HOME) {
                HomeScreen(
                    onOpenPack = { packId ->
                        navController.navigate("pack/$packId")
                    },
                    onCreateSticker = {
                        navController.navigate(StickLyRoutes.EDITOR_REF) { launchSingleTop = true }
                    },
                )
            }
            composable(
                route = StickLyRoutes.EDITOR,
                arguments = listOf(navArgument("packId") { type = NavType.StringType; defaultValue = "" }),
            ) { entry ->
                val packId = entry.arguments?.getString("packId").orEmpty()
                EditorScreen(
                    packId = packId,
                    onBack = { navController.popBackStack() },
                    onSaved = { savedPackId ->
                        navController.navigate("pack/$savedPackId") {
                            popUpTo(navController.graph.findStartDestination().id)
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(
                route = StickLyRoutes.PACK,
                arguments = listOf(navArgument("packId") { type = NavType.StringType }),
            ) { entry ->
                val packId = entry.arguments?.getString("packId").orEmpty()
                PackOverviewScreen(
                    packId = packId,
                    onBack = { navController.popBackStack() },
                    onAddSticker = { packIdToEdit ->
                        navController.navigate("${StickLyRoutes.EDITOR_REF}$packIdToEdit")
                    },
                )
            }
            composable(StickLyRoutes.INFO) {
                InfoScreen()
            }
        }
    }
}

@Composable
private fun AppBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    onCreateFresh: () -> Unit,
) {
    NavigationBar {
        NavigationBarItem(
            selected = currentRoute == StickLyRoutes.HOME,
            onClick = { onNavigate(StickLyRoutes.HOME) },
            icon = { Icon(Icons.Filled.Collections, contentDescription = null) },
            label = { Text("Paket") },
        )
        NavigationBarItem(
            selected = currentRoute?.startsWith("editor") == true,
            onClick = onCreateFresh,
            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
            label = { Text("Buat") },
        )
        NavigationBarItem(
            selected = currentRoute == StickLyRoutes.INFO,
            onClick = { onNavigate(StickLyRoutes.INFO) },
            icon = { Icon(Icons.Filled.Info, contentDescription = null) },
            label = { Text("Info") },
        )
    }
}