package live.ditto.quickstart.tasks.list

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import live.ditto.quickstart.tasks.R
import live.ditto.quickstart.tasks.data.Task
import java.util.UUID

/**
 * A row in the task list screen
 */
@Composable
fun TaskRow(
    task: Task,
    onToggle: ((task: Task) -> Unit)? = null,
    onClickEdit: ((task: Task) -> Unit)? = null,
    onClickDelete: ((task: Task) -> Unit)? = null
) {

    val iconId =
        if (task.done) R.drawable.ic_baseline_circle_24 else R.drawable.ic_outline_circle_24
    val color = if (task.done) R.color.blue_200 else R.color.gray
    val textDecoration = if (task.done) TextDecoration.LineThrough else TextDecoration.None
    ListItem(
        headlineContent = {
            Text(
                text = task.title,
                textDecoration = textDecoration
            )
        },
        leadingContent = {
            Image(
                ImageVector.vectorResource(
                    id = iconId
                ),
                "Toggle",
                colorFilter = ColorFilter.tint(colorResource(id = color)),
                modifier = Modifier
                    .clickable { onToggle?.invoke(task) },
                alignment = Alignment.CenterEnd
            )
        },
        trailingContent = {
            Row {
                IconButton(onClick = { onClickEdit?.invoke(task) }) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = "Delete"
                    )
                }
                IconButton(onClick = { onClickDelete?.invoke(task) }) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Delete",
                    )
                }
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun TaskRowPreview() {
    Column {
        TaskRow(task = Task(UUID.randomUUID().toString(), "Get Milk", true, false))
        TaskRow(task = Task(UUID.randomUUID().toString(), "Do Homework", false, false))
        TaskRow(task = Task(UUID.randomUUID().toString(), "Take out trash", true, false))
    }
}
