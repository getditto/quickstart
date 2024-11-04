package live.ditto.quickstart.tasks.edit

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun EditForm(
    canDelete: Boolean,
    title: String,
    onTitleTextChange: ((title: String) -> Unit)? = null,
    done: Boolean = false,
    onDoneChanged: ((done: Boolean) -> Unit)? = null,
    onSaveButtonClicked: (() -> Unit)? = null,
    onDeleteButtonClicked: (() -> Unit)? = null,
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(text = "Title:")
        TextField(
            value = title,
            onValueChange = { onTitleTextChange?.invoke(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            Arrangement.SpaceBetween
        ) {
            Text(text = "Is Complete:")
            Switch(checked = done, onCheckedChange = { onDoneChanged?.invoke(it) })
        }
        Button(
            onClick = {
                onSaveButtonClicked?.invoke()
            },
            modifier = Modifier
                .padding(bottom = 12.dp)
                .fillMaxWidth(),
        ) {
            Text(
                text = "Save",
                modifier = Modifier.padding(8.dp)
            )
        }
        if (canDelete) {
            Button(
                onClick = {
                    onDeleteButtonClicked?.invoke()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Red,
                    contentColor = Color.White),
                modifier = Modifier
                    .fillMaxWidth(),
            ) {
                Text(
                    text = "Delete",
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}

@Preview(
    showBackground = true,
    device = Devices.PIXEL_3
)
@Composable
fun EditFormPreview() {
    EditForm(canDelete = true, "Hello")
}
