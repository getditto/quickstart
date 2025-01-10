
using System.Collections.ObjectModel;
using System.Text.Json;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using DittoMauiTasksApp.Utils;
using DittoSDK;

namespace DittoMauiTasksApp.ViewModels
{
    public partial class TasksPageviewModel : ObservableObject
    {
        private readonly Ditto ditto;
        private readonly IPopupService popupService;

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
                {"body", taskData},
                {"isCompleted", false},
                { "isDeleted", false }
            };

            await ditto.Store.ExecuteAsync($"INSERT INTO {DittoTask.CollectionName} DOCUMENTS (:doc1)", new Dictionary<string, object>()
            {
                { "doc1", dict }
            });
        }

        [RelayCommand]
        private void DeleteTask(DittoTask task)
        {
            var updateQuery = $"UPDATE {DittoTask.CollectionName} " +
                "SET isDeleted = true " +
                $"WHERE _id = '{task.Id}'";
            ditto.Store.ExecuteAsync(updateQuery);
        }

        private void ObserveDittoTasksCollection()
        {
            var query = $"SELECT * FROM {DittoTask.CollectionName} WHERE isDeleted = false";

            ditto.Sync.RegisterSubscription(query);
            ditto.Store.RegisterObserver(query, storeObservationHandler: async (queryResult) =>
            {
                Tasks = new ObservableCollection<DittoTask>(queryResult.Items.ConvertAll(d =>
                {
                    return JsonSerializer.Deserialize<DittoTask>(d.JsonString());
                }));
            });

            ditto.Store.ExecuteAsync($"EVICT FROM {DittoTask.CollectionName} WHERE isDeleted = false");
        }
    }
}
