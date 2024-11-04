package live.ditto.quickstart.tasks.list

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import live.ditto.quickstart.tasks.DittoHandler.Companion.ditto
import live.ditto.quickstart.tasks.data.Task
import java.lang.Exception

class TasksListScreenViewModel: ViewModel() {
    val tasks: MutableLiveData<List<Task>> = MutableLiveData(emptyList())

    init {
        val query = "SELECT * FROM tasks WHERE isDeleted != true"
        ditto.sync.registerSubscription(query)
        ditto.store.registerObserver(query) { result ->
            val list = result.items.map { item -> Task.fromJson(item.jsonString()) }
             tasks.postValue(list)
        }
    }

    fun toggle(taskId: String) {
        viewModelScope.launch {
            try {
                val doc = ditto.store.execute(
                    "SELECT * FROM tasks WHERE _id = :_id",
                    mapOf("_id" to taskId)
                ).items.first()

                val isCompleted = doc.value["isCompleted"] as Boolean

                ditto.store.execute(
                    "UPDATE tasks SET isCompleted = :toggled WHERE _id = :_id",
                    mapOf(
                        "toggled" to !isCompleted,
                        "_id" to taskId
                    )
                )
            } catch (e: Exception) {
                Log.e("ERROR:", e.message.toString())
            }
        }
    }
}