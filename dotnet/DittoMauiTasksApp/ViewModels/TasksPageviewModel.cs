
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
                    logger.LogError($"Error: Unable to start Ditto sync: {e.Message}");
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

            await ditto.Store.ExecuteAsync("INSERT INTO tasks DOCUMENTS (:doc)", new Dictionary<string, object>()
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
                                    if (existingTask.Id != newTask.Id)
                                        existingTask.Id = newTask.Id;
                                    if (existingTask.Title != newTask.Title)
                                        existingTask.Title = newTask.Title;
                                    if (existingTask.Done != newTask.Done)
                                        existingTask.Done = newTask.Done;
                                    if (existingTask.Deleted != newTask.Deleted)
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
                            logger.LogError($"Error: Unable to update list view model: {e.Message}");
                        }
                    });
                }
                catch (Exception e)
                {
                    logger.LogError($"Error: Unable to process tasks collection change: {e.Message}");
                }
            });
        }
    }
}
