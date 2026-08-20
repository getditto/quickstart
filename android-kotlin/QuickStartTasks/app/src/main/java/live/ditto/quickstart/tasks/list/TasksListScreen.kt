package live.ditto.quickstart.tasks.list

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import live.ditto.quickstart.tasks.BuildConfig
import live.ditto.quickstart.tasks.R
import live.ditto.quickstart.tasks.data.Task
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksListScreen(navController: NavController) {
    val tasksListViewModel: TasksListScreenViewModel = viewModel()
    val tasks: List<Task> by tasksListViewModel.tasks.collectAsStateWithLifecycle()
    val syncEnabled: Boolean by tasksListViewModel.syncEnabled.collectAsStateWithLifecycle()

    var showDeleteDialog by remember { mutableStateOf(false) }
    var deleteDialogTaskId by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    ) {
                        Column {
                            Text(
                                text = stringResource(id = R.string.tasks_title),
                                style = MaterialTheme.typography.titleMedium
                            )
                            if (BuildConfig.DEBUG) {
                                Text(
                                    text = stringResource(
                                        id = R.string.tasks_app_id,
                                        BuildConfig.DITTO_DATABASE_ID
                                    ),
                                    style = MaterialTheme.typography.labelSmall
                                )
                                Text(
                                    text = stringResource(
                                        id = R.string.tasks_token,
                                        BuildConfig.DITTO_DEVELOPMENT_TOKEN
                                    ),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(id = R.string.tasks_sync_label),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(end = 10.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Switch(
                            checked = syncEnabled,
                            onCheckedChange = { isChecked ->
                                tasksListViewModel.setSyncEnabled(isChecked)
                            }
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                icon = {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                },
                text = {
                    Text(
                        text = stringResource(id = R.string.tasks_new),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                },
                onClick = { navController.navigate("tasks/edit") },
                elevation = FloatingActionButtonDefaults.elevation(8.dp),
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        },
        floatingActionButtonPosition = FabPosition.End,
        content = { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                TasksList(
                    tasks = tasks,
                    onToggle = { tasksListViewModel.toggle(it) },
                    onClickEdit = {
                        navController.navigate("tasks/edit/${it}")
                    },
                    onClickDelete = {
                        deleteDialogTaskId = it
                        showDeleteDialog = true
                    }
                )
            }
        }
    )

    // Alert displayed if user taps a Delete icon for a list item
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = stringResource(id = R.string.cd_warning),
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text(
                    text = stringResource(id = R.string.tasks_confirm_delete_title),
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Text(text = stringResource(id = R.string.tasks_confirm_delete_message))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        tasksListViewModel.delete(deleteDialogTaskId)
                    }
                ) {
                    Text(stringResource(id = R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(id = R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
fun TasksList(
    tasks: List<Task>,
    onToggle: ((task: Task) -> Unit)? = null,
    onClickEdit: ((taskId: String) -> Unit)? = null,
    onClickDelete: ((taskId: String) -> Unit)? = null,
) {
    LazyColumn {
        items(tasks, key = { it._id }) { task ->
            TaskRow(
                task = task,
                onToggle = { onToggle?.invoke(it) },
                onClickEdit = { onClickEdit?.invoke(it._id) },
                onClickDelete = { onClickDelete?.invoke(it._id) }
            )
        }
    }
}

@Preview(
    showBackground = true,
    showSystemUi = true,
    device = Devices.PIXEL_3
)
@Composable
fun TasksListPreview() {
    TasksList(
        tasks = listOf(
            Task(UUID.randomUUID().toString(), "Get Milk", true, false),
            Task(UUID.randomUUID().toString(), "Get Oats", false, false),
            Task(UUID.randomUUID().toString(), "Get Berries", true, false),
        )
    )
}
