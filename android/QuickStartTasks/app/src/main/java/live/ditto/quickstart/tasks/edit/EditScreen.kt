package live.ditto.quickstart.tasks.edit

import android.annotation.SuppressLint
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditScreen(navController: NavController, taskId: String?) {
    val editScreenViewModel: EditScreenViewModel = viewModel()
    editScreenViewModel.setupWithTask(id = taskId)

    val topBarTitle = if (taskId == null) "New Task" else "Edit Task"

    val body: String by editScreenViewModel.body.observeAsState("")
    val isCompleted: Boolean by editScreenViewModel.isCompleted.observeAsState(initial = false)
    val canDelete: Boolean by editScreenViewModel.canDelete.observeAsState(initial = false)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(topBarTitle) }
            )
        },
        content = {
            EditForm(
                canDelete = canDelete,
                body = body,
                onBodyTextChange = { editScreenViewModel.body.value = it },
                isCompleted = isCompleted,
                onIsCompletedChanged = { editScreenViewModel.isCompleted.value = it },
                onSaveButtonClicked = {
                    editScreenViewModel.save()
                    navController.popBackStack()
                },
                onDeleteButtonClicked = {
                    editScreenViewModel.delete()
                    navController.popBackStack()
                }
            )
        }
    )
}

