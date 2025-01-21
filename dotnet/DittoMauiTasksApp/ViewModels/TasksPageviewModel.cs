
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
    public partial class TasksPageviewModel : ObservableObject, IDisposable
    {
        private readonly Ditto ditto;
        private readonly IPopupService popupService;
        private readonly ILogger<TasksPageviewModel> logger;

        private readonly string query = "SELECT * FROM tasks WHERE NOT deleted";
        private DittoStoreObserver storeObserver;
        private DittoSyncSubscription syncSubscription;

        public string AppIdText { get; } = $"App ID: {EnvConstants.DITTO_APP_ID}";
        public string TokenText { get; } = $"Token: {EnvConstants.DITTO_PLAYGROUND_TOKEN}";

        [ObservableProperty]
        ObservableCollection<DittoTask> tasks;

        [ObservableProperty]
        private bool isSyncEnabled = true;

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
                    ObserveDittoTasksCollection();
                    StartSync();
                }
                catch (Exception e)
                {
                    logger.LogError($"TasksPageviewModel: Unable to start Ditto sync: {e.Message}");
                }
            });
        }

        public void Dispose()
        {
            if (syncSubscription != null)
            {
                try
                {
                    syncSubscription.Cancel();
                }
                catch (Exception e)
                {
                    logger.LogError($"TasksPageviewModel: Error cancelling sync subscription: {e.Message}");
                }
                syncSubscription = null;
            }

            if (storeObserver != null)
            {
                try
                {
                    storeObserver.Cancel();
                }
                catch (Exception e)
                {
                    logger.LogError($"TasksPageviewModel: Error cancelling store observer: {e.Message}");
                }
                storeObserver = null;
            }
        }

        [RelayCommand]
        private async Task AddTaskAsync()
        {
            try
            {
                var title = await popupService.DisplayPromptAsync(
                    "Add Task", "Add a new task:", "Task title");

                if (string.IsNullOrWhiteSpace(title))
                {
                    // nothing was entered
                    return;
                }
                title.Trim();

                var doc = new Dictionary<string, object>
                {
                    {"title", title},
                    {"done", false},
                    {"deleted", false }
                };
                var insertCommand = "INSERT INTO tasks DOCUMENTS (:doc)";
                await ditto.Store.ExecuteAsync(insertCommand, new Dictionary<string, object>()
                {
                    { "doc", doc }
                });
            }
            catch (Exception e)
            {
                logger.LogError($"TasksPageviewModel: Error adding task: {e.Message}");
            }
        }

        [RelayCommand]
        private async Task EditTaskAsync(DittoTask task)
        {
            try
            {
                var newTitle = await popupService.DisplayPromptAsync(
                    "Edit Task", "Change task title:", "Task title",
                    initialValue: task.Title);

                if (string.IsNullOrWhiteSpace(newTitle))
                {
                    // nothing was entered
                    return;
                }
                newTitle.Trim();

                var updateQuery = "UPDATE tasks " +
                    "SET title = :title " +
                    "WHERE _id = :id";
                await ditto.Store.ExecuteAsync(updateQuery, new Dictionary<string, object>()
                {
                    {"title", newTitle},
                    {"id", task.Id}
                });
            }
            catch (Exception e)
            {
                logger.LogError($"TasksPageviewModel: Error editing task: {e.Message}");
            }
        }

        [RelayCommand]
        private void DeleteTask(DittoTask task)
        {
            try
            {
                var updateQuery = "UPDATE tasks " +
                    "SET deleted = true " +
                    "WHERE _id = :id";
                ditto.Store.ExecuteAsync(updateQuery, new Dictionary<string, object>()
                {
                    { "id", task.Id }
                });
            }
            catch (Exception e)
            {
                logger.LogError($"TasksPageviewModel: Error deleting task: {e.Message}");
            }
        }

        [RelayCommand]
        private Task UpdateTaskDoneAsync(DittoTask task)
        {
            try
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
                    }
                    catch (Exception e)
                    {
                        logger.LogError($"TasksPageviewModel: Error updating task done state for {taskId}: {e.Message}");
                    }
                });
            }
            catch (Exception e)
            {
                logger.LogError($"TasksPageviewModel: Error updating task done state: {e.Message}");
            }
            return Task.CompletedTask;
        }

        private void ObserveDittoTasksCollection()
        {
            storeObserver = ditto.Store.RegisterObserver(query, async (queryResult) =>
            {
                try
                {
                    var newTasks = queryResult.Items.Select(d =>
                        JsonSerializer.Deserialize<DittoTask>(d.JsonString())
                    ).OrderBy(t => t.Id).ToList();

                    MainThread.BeginInvokeOnMainThread(() =>
                    {
                        try
                        {
                            if (Tasks == null)
                            {
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
                                    existingTask.Id = newTask.Id;
                                    existingTask.Title = newTask.Title;
                                    existingTask.Done = newTask.Done;
                                    existingTask.Deleted = newTask.Deleted;
                                }

                                if (oldCount < newCount)
                                {
                                    for (var i = oldCount; i < newCount; i++)
                                    {
                                        Tasks.Add(newTasks[i]);
                                    }
                                }
                                else if (oldCount > newCount)
                                {
                                    for (var i = oldCount - 1; i >= newCount; i--)
                                    {
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

        partial void OnIsSyncEnabledChanged(bool value)
        {
            if (value)
            {
                StartSync();
            }
            else
            {
                StopSync();
            }
        }

        private void StartSync()
        {
            try
            {
                ditto.StartSync();
                syncSubscription = ditto.Sync.RegisterSubscription(query);
            }
            catch (Exception e)
            {
                logger.LogError($"TasksPageviewModel: Error starting Ditto sync: {e.Message}");
            }
        }

        private void StopSync()
        {
            if (syncSubscription != null)
            {
                try
                {
                    syncSubscription.Cancel();
                }
                catch (Exception e)
                {
                    logger.LogError($"TasksPageviewModel: Error cancelling sync subscription: {e.Message}");
                }
                syncSubscription = null;
            }

            try
            {
                ditto.StopSync();
            }
            catch (Exception e)
            {
                logger.LogError($"TasksPageviewModel: Error stopping Ditto sync: {e.Message}");
            }
        }
    }
}
