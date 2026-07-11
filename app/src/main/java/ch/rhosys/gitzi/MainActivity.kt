package ch.rhosys.gitzi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
        super.onCreate(savedInstanceState)

        setContent {
            GitziTheme {
                val nullableSettings: Flow<ConnectionSettings?> = connectionSettings.settings
                val settings by nullableSettings.collectAsState(initial = null)

                when (val current = settings) {
                    null -> FullScreenLoading()
                    else -> GitziRoot(startDestination = if (current.isPaired) Screen.Board.route else Screen.Setup.route)
                }
            }
        }
    }
}

private data class BottomTab(val screen: Screen, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val BOTTOM_TABS =
    listOf(
        BottomTab(Screen.Board, "Board", Icons.Default.Dashboard),
        BottomTab(Screen.Epics, "Epics", Icons.Default.ListAlt),
        BottomTab(Screen.Review, "Review", Icons.Default.FactCheck),
        BottomTab(Screen.Chat, "Chat", Icons.Default.Chat),
        BottomTab(Screen.Settings, "Settings", Icons.Default.Settings),
    )

@androidx.compose.runtime.Composable
private fun GitziRoot(startDestination: String) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = BOTTOM_TABS.any { it.screen.route == currentRoute }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    BOTTOM_TABS.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.screen.route,
                            onClick = {
                                navController.navigate(tab.screen.route) {
                                    popUpTo(Screen.Board.route) { saveState = true }
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
