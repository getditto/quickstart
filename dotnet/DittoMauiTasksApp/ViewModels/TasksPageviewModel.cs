
using System.Collections.ObjectModel;
using System.Text.Json;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using DittoMauiTasksApp.Utils;
using DittoSDK;
using Generated;

namespace DittoMauiTasksApp.ViewModels
{
    public partial class TasksPageviewModel : ObservableObject
    {
        private readonly Ditto ditto;
        private readonly IPopupService popupService;

        public string AppIdText { get; } = "App ID: " + EnvConstants.DITTO_APP_ID;
        public string TokenText { get; } = "Token: " + EnvConstants.DITTO_PLAYGROUND_TOKEN;

        [ObservableProperty]
        ObservableCollection<DittoTask> tasks;

        public TasksPageviewModel(Ditto ditto, IPopupService popupService)
        {
            this.ditto = ditto;
            this.popupService = popupService;

            PermissionHelper.CheckPermissions().ContinueWith(async t =>
            {
                ditto.DisableSyncWithV3();
                ditto.StartSync();

                ObserveDittoTasksCollection();
            });
        }

        [RelayCommand]
        private async Task AddTaskAsync()
        {
            string taskData = await popupService.DisplayPromptAsync("Add Task", "Add a new task:");

            if (taskData == null)
            {
                //nothing was entered.
                return;
            }

            var dict = new Dictionary<string, object>
            {
                {"title", taskData},
                {"done", false},
                {"deleted", false }
            };

            await ditto.Store.ExecuteAsync($"INSERT INTO tasks DOCUMENTS (:doc1)", new Dictionary<string, object>()
            {
                { "doc1", dict }
            });
        }

        [RelayCommand]
        private void DeleteTask(DittoTask task)
        {
            var updateQuery = $"UPDATE tasks " +
                "SET deleted = true " +
                "WHERE _id = :id";
            ditto.Store.ExecuteAsync(updateQuery, new Dictionary<string, object>()
            {
                { "id", task.Id }
            });
        }

        private void ObserveDittoTasksCollection()
        {
            var query = $"SELECT * FROM tasks WHERE NOT deleted";

            ditto.Sync.RegisterSubscription(query);
            ditto.Store.RegisterObserver(query, storeObservationHandler: async (queryResult) =>
            {
                Tasks = new ObservableCollection<DittoTask>(queryResult.Items.ConvertAll(d =>
                {
                    var json = d.JsonString();
                    var task = JsonSerializer.Deserialize<DittoTask>(json);
                    return task;
                }));
            });
        }
    }
}
