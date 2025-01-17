
using System.Collections.ObjectModel;
using System.Text.Json;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using DittoMauiTasksApp.Utils;
using DittoSDK;
using Generated;
using Microsoft.Extensions.Logging;

namespace DittoMauiTasksApp.ViewModels
{
    public partial class TasksPageviewModel : ObservableObject
    {
        private readonly Ditto ditto;
        private readonly IPopupService popupService;
        private readonly ILogger<TasksPageviewModel> logger;

        public string AppIdText { get; } = $"App ID: {EnvConstants.DITTO_APP_ID}";
        public string TokenText { get; } = $"Token: {EnvConstants.DITTO_PLAYGROUND_TOKEN}";

        static string Timestamp() => DateTime.Now.ToString("yyyy-MM-dd HH:mm:ss.fff");

        [ObservableProperty]
        ObservableCollection<DittoTask> tasks;

        public TasksPageviewModel(
            Ditto ditto, IPopupService popupService, ILogger<TasksPageviewModel> logger)
        {
            this.ditto = ditto;
            this.popupService = popupService;
            this.logger = logger;

            PermissionHelper.CheckPermissions().ContinueWith(async t =>
            {
                try
                {
                    ditto.DisableSyncWithV3();
                    ditto.StartSync();

                    ObserveDittoTasksCollection();
                }
                catch (Exception e)
                {
                    logger.LogError($"TasksPageviewModel: Unable to start Ditto sync: {e.Message}");
                }
            });
        }

        [RelayCommand]
        private async Task AddTaskAsync()
        {
            var title = await popupService.DisplayPromptAsync(
                "Add Task", "Add a new task:", "Task title");

            if (string.IsNullOrWhiteSpace(title))
            {
                //nothing was entered.
                return;
            }
            title.Trim();

            var doc = new Dictionary<string, object>
            {
                {"title", title},
                {"done", false},
                {"deleted", false }
            };
            var query = "INSERT INTO tasks DOCUMENTS (:doc)";
            await ditto.Store.ExecuteAsync(query, new Dictionary<string, object>()
            {
                { "doc", doc }
            });
        }

        [RelayCommand]
        private async Task EditTaskAsync(DittoTask task)
        {
            var newTitle = await popupService.DisplayPromptAsync(
                "Edit Task", "Change task title:", "Task title", initialValue: task.Title);

            if (string.IsNullOrWhiteSpace(newTitle))
            {
                //nothing was entered.
                return;
            }
            newTitle.Trim();

            var updateQuery = "UPDATE tasks " +
                "SET title = :title " +
                "WHERE _id = :id";
            await ditto.Store.ExecuteAsync(updateQuery, new Dictionary<string, object>
            {
                {"title", newTitle},
                {"id", task.Id}
            });
        }

        [RelayCommand]
        private void DeleteTask(DittoTask task)
        {
            var updateQuery = "UPDATE tasks " +
                "SET deleted = true " +
                "WHERE _id = :id";
            ditto.Store.ExecuteAsync(updateQuery, new Dictionary<string, object>()
            {
                { "id", task.Id }
            });
        }

        [RelayCommand]
        private Task UpdateTaskDoneAsync(DittoTask task)
        {
            if (task == null)
            {
                logger.LogWarning("TasksPageviewModel: UpdateTaskDoneAsync called with null task");
                return Task.CompletedTask;
            }

            var taskId = task.Id;
            var newDoneState = task.Done;

            // Fire-and-forget the Ditto update to avoid blocking the UI
            // thread while handling a checkbox change
            _ = Task.Run(async () =>
            {
                try
                {
                    logger.LogDebug($"{Timestamp()}: Updating task done state in Ditto: {taskId} to {newDoneState}");

                    // Update the task done state only if it has changed, to
                    // avoid unnecessary calls to the store observer callback.
                    var updateQuery = "UPDATE tasks " +
                        "SET done = :newDoneState " +
                        "WHERE _id = :id AND done != :newDoneState";
                    var result = await ditto.Store.ExecuteAsync(updateQuery, new Dictionary<string, object>
                    {
                        { "newDoneState", newDoneState },
                        { "id", taskId }
                    });
                    logger.LogDebug($"T{Timestamp()}: Update result row count: {result.MutatedDocumentIds.Count}");
                }
                catch (Exception e)
                {
                    logger.LogError($"TasksPageviewModel: Error updating task done state for {taskId}: {e.Message}");
                }
            });

            logger.LogDebug($"{Timestamp()}: returning from UpdateTaskDoneAsync for task {taskId}");
            return Task.CompletedTask;
        }

        private void ObserveDittoTasksCollection()
        {
            var query = "SELECT * FROM tasks WHERE NOT deleted";

            ditto.Sync.RegisterSubscription(query);
            ditto.Store.RegisterObserver(query, async (queryResult) =>
            {
                try
                {
                    var newTasks = queryResult.Items.Select(d =>
                        JsonSerializer.Deserialize<DittoTask>(d.JsonString())
                    ).OrderBy(t => t.Id).ToList();

                    logger.LogDebug($"{Timestamp()}: Observer callback invoked; task count: {newTasks.Count}");

                    MainThread.BeginInvokeOnMainThread(() =>
                    {
                        try
                        {
                            if (Tasks == null)
                            {
                                logger.LogDebug($"{Timestamp()}: Creating new observable tasks collection");
                                Tasks = new ObservableCollection<DittoTask>(newTasks);
                            }
                            else
                            {
                                var oldCount = Tasks.Count;
                                var newCount = newTasks.Count;
                                var minCount = Math.Min(oldCount, newCount);

                                for (var i = 0; i < minCount; i++)
                                {
                                    var existingTask = Tasks[i];
                                    var newTask = newTasks[i];
                                    logger.LogDebug($"{Timestamp()}: Updating task: {existingTask.Id}->{newTask.Id} at index {i}");
                                    if (existingTask.Id != newTask.Id) {
                                        existingTask.Id = newTask.Id;
                                    }
                                    if (existingTask.Title != newTask.Title) {
                                        logger.LogDebug($"{Timestamp()}: Updating task title: {newTask.Id} to \"{newTask.Title}\"");
                                        existingTask.Title = newTask.Title;
                                    }
                                    if (existingTask.Done != newTask.Done) {
                                        logger.LogDebug($"{Timestamp()}: Updating task done state: {newTask.Id} to {newTask.Done}");
                                        existingTask.Done = newTask.Done;
                                    } else {
                                        logger.LogDebug($"{Timestamp()}: Task done state unchanged: {newTask.Id}");
                                    }
                                    if (existingTask.Deleted != newTask.Deleted) {
                                        logger.LogDebug($"{Timestamp()}: Updating task deleted state: {newTask.Id} to {newTask.Deleted}");
                                        existingTask.Deleted = newTask.Deleted;
                                    }
                                }

                                if (oldCount < newCount)
                                {
                                    for (var i = oldCount; i < newCount; i++)
                                    {
                                        logger.LogDebug($"{Timestamp()}: Adding new task to collection: {newTasks[i].Id}");
                                        Tasks.Add(newTasks[i]);
                                    }
                                }
                                else if (oldCount > newCount)
                                {
                                    for (var i = oldCount - 1; i >= newCount; i--)
                                    {
                                        logger.LogDebug($"{Timestamp()}: Removing task from collection: {Tasks[i].Id}");
                                        Tasks.RemoveAt(i);
                                    }
                                }
                            }
                        }
                        catch (Exception e)
                        {
                            logger.LogError($"TasksPageviewModel: Error: Unable to update list view model: {e.Message}");
                        }
                    });
                }
                catch (Exception e)
                {
                    logger.LogError($"TasksPageviewModel: Error: Unable to process tasks collection change: {e.Message}");
                }
            });
        }
    }
}
