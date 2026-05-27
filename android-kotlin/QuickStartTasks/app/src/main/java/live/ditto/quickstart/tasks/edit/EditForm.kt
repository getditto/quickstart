package live.ditto.quickstart.tasks.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import live.ditto.quickstart.tasks.R

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
        Text(text = stringResource(id = R.string.edit_field_title))
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
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = stringResource(id = R.string.edit_field_is_complete))
            Switch(checked = done, onCheckedChange = { onDoneChanged?.invoke(it) })
        }
        Button(
            onClick = { onSaveButtonClicked?.invoke() },
            modifier = Modifier
                .padding(bottom = 12.dp)
                .fillMaxWidth(),
        ) {
            Text(
                text = stringResource(id = R.string.action_save),
                modifier = Modifier.padding(8.dp)
            )
        }
        if (canDelete) {
            Button(
                onClick = { onDeleteButtonClicked?.invoke() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(id = R.string.action_delete),
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
    EditForm(canDelete = true, title = "Hello")
}
