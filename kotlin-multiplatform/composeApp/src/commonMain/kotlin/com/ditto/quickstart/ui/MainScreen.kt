package com.ditto.quickstart.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ditto.quickstart.data.Task
import com.ditto.quickstart.ui.components.Loading
import com.ditto.quickstart.ui.components.RemoveTaskDialog
import com.ditto.quickstart.ui.components.TopBar
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
) {
    val mainScreenViewModel = koinViewModel<MainScreenViewModel>()
    val mainScreenState by mainScreenViewModel.state.collectAsStateWithLifecycle()
    var showFloatingActionButton by remember { mutableStateOf(false) }
    var openRemoveDialogForTask: Task? by remember { mutableStateOf(null) }
    val navController = rememberNavController()
    val snackBarHostState = remember { SnackbarHostState() }

    LaunchedEffect(key1 = mainScreenState.errorMessage) {
        val errorMessage = mainScreenState.errorMessage
        if (errorMessage != null) {
            snackBarHostState.showSnackbar(errorMessage)
        }
    }

    MaterialTheme {
        Scaffold(
            modifier = modifier,
            topBar = {
                TopBar(
                    isLoading = mainScreenState.isLoading,
                    appId = mainScreenState.appId,
                    appToken = mainScreenState.appToken,
                    isSyncEnabled = mainScreenState.isSyncEnabled,
                    onSyncChange = mainScreenViewModel::onSyncChange
                )
            },
            floatingActionButton = {
                if (!showFloatingActionButton) return@Scaffold

                FloatingActionButton(
                    onClick = {
                        navController.navigate("addEdit/")
                    },
                ) {
                    Icon(Icons.Filled.Add, "Add Task")
                }
            },
            snackbarHost = { SnackbarHost(hostState = snackBarHostState) }
        ) {
            if (mainScreenState.isLoading) {
                Loading()
                return@Scaffold
            }

            val taskToBeRemoved = openRemoveDialogForTask
            if (taskToBeRemoved != null) {
                RemoveTaskDialog(
                    taskTitle = taskToBeRemoved.title,
                    onConfirm = {
                        mainScreenViewModel.removeTask(taskToBeRemoved)
                        openRemoveDialogForTask = null
                    },
                    onDismiss = { openRemoveDialogForTask = null }
                )
            }

            NavHost(
                navController = navController,
                startDestination = "tasks"
            ) {
                composable("tasks") {
                    showFloatingActionButton = true

                    val taskListScreenViewModel = koinViewModel<TaskListScreenViewModel>()
                    val taskListState by taskListScreenViewModel.state.collectAsStateWithLifecycle()

                    TaskListScreen(
                        modifier = Modifier.fillMaxSize(),
                        state = taskListState,
                        onCheck = { task, checked ->
                            taskListScreenViewModel.onCheck(task, checked)
                        },
                        onEdit = { task ->
                            navController.navigate("addEdit/${task.id}")
                        },
                        onRemove = { task ->
                            openRemoveDialogForTask = task
                        }
                    )
                }

                composable("addEdit/{taskId}") { backStackEntry ->
                    showFloatingActionButton = false
                    val taskId = backStackEntry.arguments?.getString("taskId")
                    val taskAddEditScreenViewModel = koinViewModel<TaskAddEditScreenViewModel>()
                    val taskEditState by taskAddEditScreenViewModel.state.collectAsStateWithLifecycle()

                    TaskAddEditScreen(
                        modifier = Modifier.fillMaxSize(),
                        state = taskEditState,
                        onCancel = { navController.popBackStack() },
                        onSubmit = { title ->
                            taskAddEditScreenViewModel.onSubmitClick(
                                taskId = taskId,
                                title = title
                            )
                            navController.popBackStack()
                        }
                    )
                }
            }
        }
    }
}
