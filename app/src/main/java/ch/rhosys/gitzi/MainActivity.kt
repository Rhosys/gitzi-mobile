package ch.rhosys.gitzi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import ch.rhosys.gitzi.domain.model.ConnectionSettings
import ch.rhosys.gitzi.domain.repository.ConnectionSettingsRepository
import ch.rhosys.gitzi.ui.common.FullScreenLoading
import ch.rhosys.gitzi.ui.navigation.AppNavHost
import ch.rhosys.gitzi.ui.navigation.Screen
import ch.rhosys.gitzi.ui.theme.GitziTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var connectionSettings: ConnectionSettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            GitziTheme {
                val nullableSettings: Flow<ConnectionSettings?> = connectionSettings.settings
                val settings by nullableSettings.collectAsState(initial = null)

                when (val current = settings) {
                    null -> FullScreenLoading()
                    else -> GitziRoot(startDestination = if (current.isPaired) Screen.Chat.route else Screen.Setup.route)
                }
            }
        }
    }
}

private data class BottomTab(val screen: Screen, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val BOTTOM_TABS =
    listOf(
        BottomTab(Screen.Chat, "Chat", Icons.Default.Chat),
        BottomTab(Screen.Board, "Board", Icons.Default.FactCheck),
        BottomTab(Screen.Settings, "Settings", Icons.Default.Settings),
    )

private data class OverflowItem(val screen: Screen, val label: String)

private val OVERFLOW_ITEMS =
    listOf(
        OverflowItem(Screen.Epics, "Epics"),
        OverflowItem(Screen.Review, "Review"),
    )

private val CHROME_ROUTES = (BOTTOM_TABS.map { it.screen.route } + OVERFLOW_ITEMS.map { it.screen.route }).toSet()

@Composable
private fun GitziRoot(startDestination: String) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showChrome = currentRoute in CHROME_ROUTES

    Scaffold(
        topBar = {
            if (showChrome) {
                var menuExpanded by remember { mutableStateOf(false) }
                TopAppBar(
                    title = { Text("Gitzi") },
                    actions = {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                        ) {
                            OVERFLOW_ITEMS.forEach { item ->
                                DropdownMenuItem(
                                    text = { Text(item.label) },
                                    onClick = {
                                        menuExpanded = false
                                        navController.navigate(item.screen.route) {
                                            popUpTo(Screen.Chat.route) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                )
                            }
                        }
                    },
                )
            }
        },
        bottomBar = {
            if (showChrome) {
                NavigationBar {
                    BOTTOM_TABS.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.screen.route,
                            onClick = {
                                navController.navigate(tab.screen.route) {
                                    popUpTo(Screen.Chat.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        AppNavHost(navController, startDestination = startDestination, modifier = Modifier.padding(innerPadding))
    }
}
