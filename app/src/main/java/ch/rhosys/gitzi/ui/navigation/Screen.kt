package ch.rhosys.gitzi.ui.navigation

sealed class Screen(val route: String) {
    object Setup : Screen("setup")

    object Board : Screen("board")

    object Epics : Screen("epics")

    object EpicDetail : Screen("epic/{epicId}") {
        fun route(epicId: String) = "epic/$epicId"
    }

    object TaskDetail : Screen("task/{taskId}") {
        fun route(taskId: String) = "task/$taskId"
    }

    object Review : Screen("review")

    object Chat : Screen("chat")

    object Settings : Screen("settings")

    object Logs : Screen("logs")
}
