package live.ditto.quickstart.tasks

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import live.ditto.quickstart.tasks.edit.EditScreen
import live.ditto.quickstart.tasks.list.TasksListScreen
import live.ditto.quickstart.tasks.ui.theme.QuickStartTasksTheme

@Composable
fun Root() {
    val navController = rememberNavController()

    QuickStartTasksTheme {
        // A surface container using the 'background' color from the theme
        Surface(color = MaterialTheme.colorScheme.background) {
            NavHost(navController = navController, startDestination = "tasks") {
                composable("tasks") { TasksListScreen(navController = navController) }
                composable("tasks/edit") {
                    EditScreen(navController = navController, taskJson = null)
                }
                composable("tasks/edit/{taskJson}") { backStackEntry ->
                    val taskJson: String? = backStackEntry.arguments?.getString("taskJson")
                    EditScreen(navController = navController, taskJson = taskJson)
                }
            }
        }
    }
}