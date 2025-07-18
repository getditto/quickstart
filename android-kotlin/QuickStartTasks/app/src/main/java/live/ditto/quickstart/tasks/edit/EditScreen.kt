package live.ditto.quickstart.tasks.edit

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import live.ditto.quickstart.tasks.R
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditScreen(
    navController: NavController,
    taskJson: String?,
    viewModel: EditScreenViewModel = koinViewModel()
) {
    viewModel.setupWithTask(taskJson)

    val topBarTitle = if (taskJson == null) "New Task" else "Edit Task"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(topBarTitle, color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colorResource(id = R.color.blue_700)
                )
            )
        },
        content = { padding ->
            Column(modifier = Modifier
                .fillMaxSize()
                .padding(padding)) {
                EditForm(
                    canDelete = viewModel.canDelete.value,
                    title = viewModel.title.value,
                    onTitleTextChange = { viewModel.title.value = it },
                    done = viewModel.done.value,
                    onDoneChanged = { viewModel.done.value = it },
                    onSaveButtonClicked = {
                        viewModel.save()
                        navController.popBackStack()
                    },
                    onDeleteButtonClicked = {
                        viewModel.delete()
                        navController.popBackStack()
                    }
                )
            }
        }
    )
}
