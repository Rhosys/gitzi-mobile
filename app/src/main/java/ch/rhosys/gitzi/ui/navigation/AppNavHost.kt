package ch.rhosys.gitzi.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import ch.rhosys.gitzi.ui.board.BoardScreen
import ch.rhosys.gitzi.ui.chat.ChatScreen
import ch.rhosys.gitzi.ui.epics.EpicDetailScreen
import ch.rhosys.gitzi.ui.epics.EpicsScreen
import ch.rhosys.gitzi.ui.logs.LogsScreen
import ch.rhosys.gitzi.ui.review.ReviewScreen
import ch.rhosys.gitzi.ui.settings.SettingsScreen
import ch.rhosys.gitzi.ui.setup.SetupScreen
import ch.rhosys.gitzi.ui.task.TaskDetailScreen

@Composable
fun AppNavHost(
    navController: NavHostController,
    startDestination: String,
    modifier: Modifier = Modifier,
) {
    NavHost(navController = navController, startDestination = startDestination, modifier = modifier) {
        composable(Screen.Setup.route) {
            SetupScreen(
                onSetupComplete = {
                    navController.navigate(Screen.Board.route) {
                        popUpTo(Screen.Setup.route) { inclusive = true }
                    }
                },
            )
        }
        composable(Screen.Board.route) {
            BoardScreen(onTaskClick = { taskId -> navController.navigate(Screen.TaskDetail.route(taskId)) })
        }
        composable(Screen.Epics.route) {
            EpicsScreen(onEpicClick = { epicId -> navController.navigate(Screen.EpicDetail.route(epicId)) })
        }
        composable(
            Screen.EpicDetail.route,
            arguments = listOf(navArgument("epicId") { type = NavType.StringType }),
        ) {
            EpicDetailScreen(
                onTaskClick = { taskId -> navController.navigate(Screen.TaskDetail.route(taskId)) },
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            Screen.TaskDetail.route,
            arguments = listOf(navArgument("taskId") { type = NavType.StringType }),
        ) {
            TaskDetailScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Review.route) { ReviewScreen() }
        composable(Screen.Chat.route) { ChatScreen() }
        composable(Screen.Settings.route) {
            SettingsScreen(
                onLogsClick = { navController.navigate(Screen.Logs.route) },
                onDisconnected = {
                    navController.navigate(Screen.Setup.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }
        composable(Screen.Logs.route) { LogsScreen(onBack = { navController.popBackStack() }) }
    }
}
