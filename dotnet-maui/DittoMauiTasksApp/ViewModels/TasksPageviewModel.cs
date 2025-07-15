
using System.Collections.ObjectModel;
using System.Text.Json;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using DittoMauiTasksApp.Utils;
using DittoSDK;
using Microsoft.Extensions.Logging;

namespace DittoMauiTasksApp.ViewModels
{
    public partial class TasksPageviewModel : ObservableObject
    {
        private const string SelectQuery = "SELECT * FROM tasks WHERE NOT deleted";

        private readonly Ditto ditto;
        private readonly IPopupService popupService;
        private readonly ILogger<TasksPageviewModel> logger;
        private DittoSyncSubscription syncSubscription;

        public string AppIdText { get; } = $"App ID: {MauiProgram.AppId}";
        public string TokenText { get; } = $"Token: {MauiProgram.PlaygroundToken}";

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
#if WINDOWS
                try
                {
                    Task.Run(async () =>
                    {
                        await InsertInitialTasks();
                        ObserveDittoTasksCollection();
                        StartSync();

                    });
                }
                catch (Exception e)
                {
                    logger.LogError($"TasksPageviewModel: Unable to start Ditto sync: {e.Message}");
                }
#else

                DittoSyncPermissions.RequestPermissionsAsync().ContinueWith(async t =>
                {
                    try
                    {
                        await InsertInitialTasks();
                        ObserveDittoTasksCollection();
                        StartSync();
                    }
                    catch (Exception e)
                    {
                        logger.LogError($"TasksPageviewModel: Unable to start Ditto sync: {e.Message}");
                    }
                });
#endif
            }
        private async Task InsertInitialTasks()
        {
            try
            {
                var initialTasks = new List<Dictionary<string, object>>
                {
                    new Dictionary<string, object>
                    {
                        {"_id", "50191411-4C46-4940-8B72-5F8017A04FA7"},
                        {"title", "Buy groceries"},
                        {"done", false},
                        {"deleted", false}
                    },
                    new Dictionary<string, object>
                    {
                        {"_id", "6DA283DA-8CFE-4526-A6FA-D385089364E5"},
                        {"title", "Clean the kitchen"},
                        {"done", false},
                        {"deleted", false}
                    },
                    new Dictionary<string, object>
                    {
                        {"_id", "5303DDF8-0E72-4FEB-9E82-4B007E5797F0"},
                        {"title", "Schedule dentist appointment"},
                        {"done", false},
                        {"deleted", false}
                    },
                    new Dictionary<string, object>
                    {
                        {"_id", "38411F1B-6B49-4346-90C3-0B16CE97E174"},
                        {"title", "Pay bills"},
                        {"done", false},
                        {"deleted", false}
                    }
                };

                var insertCommand = "INSERT INTO tasks INITIAL DOCUMENTS (:task)";
                foreach (var task in initialTasks)
                {
                    await ditto.Store.ExecuteAsync(insertCommand, new Dictionary<string, object>()
                    {
                        { "task", task }
                    });
                }
            }
            catch (Exception e)
            {
                logger.LogError($"TasksPageviewModel: Error adding initial tasks: {e.Message}");
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
            // Register observer, which runs against the local database on this peer
            // https://docs.ditto.live/sdk/latest/crud/observing-data-changes#setting-up-store-observers
            ditto.Store.RegisterObserver(SelectQuery, async (queryResult) =>
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
                                UpdateTasks(newTasks);
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

        private void UpdateTasks(List<DittoTask> newTasks)
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

                // Register a subscription, which determines what data syncs to this peer
                // https://docs.ditto.live/sdk/latest/sync/syncing-data#creating-subscriptions
                syncSubscription = ditto.Sync.RegisterSubscription(SelectQuery);
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
