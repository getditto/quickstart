
using System.Collections.ObjectModel;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using DittoMauiTasksApp.Data;
using DittoMauiTasksApp.Model;
using DittoMauiTasksApp.Utils;
using DittoSDK;
using Microsoft.Extensions.Logging;

namespace DittoMauiTasksApp.ViewModels
{
    public partial class TasksPageViewModel : ObservableObject
    {
        private readonly IDataService _dataService;
        private readonly IPopupService _popupService;
        private readonly ILogger<TasksPageViewModel> _logger;

        [ObservableProperty]
        ObservableCollection<DittoTask> tasks = new ObservableCollection<DittoTask>();

        [ObservableProperty]
        private bool isSyncEnabled = true;

        public TasksPageViewModel(
            IDataService dataService,
            IPopupService popupService,
            ILogger<TasksPageViewModel> logger)
        {
            this._dataService = dataService;
            this._popupService = popupService;
            this._logger = logger;
#if IOS || ANDROID || MACCATALYST
            DittoSyncPermissions.RequestPermissionsAsync().ContinueWith(t =>
            {
                try
                {
                    MainThread.BeginInvokeOnMainThread(() =>
                    {
                        _dataService.RegisterObservers(this.Tasks);
                        _dataService.StartSync();
                    });
                }
                catch (Exception e)
                {
                    logger.LogError($"TasksPageviewModel: Unable to start Ditto sync: {e.Message}");
                }
            });

#else
                try
                {
                    MainThread.BeginInvokeOnMainThread(() => {
                        _dataService.RegisterObservers(this.Tasks);
                        _dataService.StartSync();
                    });
                }
                catch (Exception e)
                {
                    logger.LogError($"TasksPageviewModel: Unable to start Ditto sync: {e.Message}");
                }

#endif
        }

        [RelayCommand]
        private async Task AddTaskAsync()
        {
            try
            {
                var title = await _popupService.DisplayPromptAsync(
                    "Add Task", "Add a new task:", "Task title");

                if (!string.IsNullOrWhiteSpace(title))
                {
                    title = title.Trim();
                }

                var doc = new Dictionary<string, object>
                {
                    {"title", title},
                    {"done", false},
                    {"deleted", false }
                };

                await _dataService.AddTask(doc);
            }
            catch (Exception e)
            {
                _logger.LogError($"TasksPageviewModel: Error adding task: {e.Message}");
            }
        }

        [RelayCommand]
        private async Task EditTaskAsync(DittoTask task)
        {
            try
            {
                var newTitle = await _popupService.DisplayPromptAsync(
                    "Edit Task", "Change task title:", "Task title",
                    initialValue: task.Title);

                if (!string.IsNullOrWhiteSpace(newTitle))
                {
                    task.Title = newTitle.Trim();
                }
                await _dataService.EditTask(task);
            }
            catch (Exception e)
            {
                _logger.LogError($"TasksPageviewModel: Error editing task: {e.Message}");
            }
        }

        [RelayCommand]
        private async Task DeleteTask(DittoTask task)
        {
            try
            {
                await _dataService.DeleteTask(task);
            }
            catch (Exception e)
            {
                _logger.LogError($"TasksPageviewModel: Error deleting task: {e.Message}");
            }
        }

        [RelayCommand]
        private async Task UpdateTaskDoneAsync(DittoTask task)
        {
            try
            {
                if (task == null)
                {
                    _logger.LogWarning("TasksPageviewModel: UpdateTaskDoneAsync called with null task");
                    return;
                }
                await _dataService.UpdateTaskDoneAsync(task);
            }
            catch (Exception e)
            {
                _logger.LogError($"TasksPageviewModel: Error updating task done state: {e.Message}");
            }
        }

        partial void OnIsSyncEnabledChanged(bool value)
        {
            if (value)
            {
                _dataService.StartSync();
            }
            else
            {
                _dataService.StopSync();
            }
        }
    }
}
