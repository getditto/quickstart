
using System.Collections.ObjectModel;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using DittoMauiTasksApp.Utils;
using DittoSDK;
using DittoSDK.Store;
using DittoSDK.Sync;
using Microsoft.Extensions.Logging;

namespace DittoMauiTasksApp.ViewModels
{
    public partial class TasksPageviewModel : ObservableObject, IDisposable
    {
        private readonly DittoManager dittoManager;
        private readonly TasksRepository tasksRepository;
        private readonly IPopupService popupService;
        private readonly ILogger<TasksPageviewModel> logger;
        private DittoStoreObserver tasksObserver;
        // Registration runs on the permissions-task continuation (thread-pool thread)
        // while Dispose() runs on the UI thread (page Unloaded), so the disposed flag and
        // the observer handoff are guarded by this lock to make the check-and-store atomic.
        private readonly object observerGate = new();
        private bool disposed;

        public string DatabaseIdText { get; }
        public string TokenText { get; }

        [ObservableProperty]
        ObservableCollection<TaskModel> tasks;

        [ObservableProperty]
        private bool isSyncEnabled = true;

        public TasksPageviewModel(
            DittoManager dittoManager,
            TasksRepository tasksRepository,
            IPopupService popupService,
            ILogger<TasksPageviewModel> logger)
        {
            this.dittoManager = dittoManager;
            this.tasksRepository = tasksRepository;
            this.popupService = popupService;
            this.logger = logger;

            DatabaseIdText = $"Database ID: {dittoManager.DatabaseId}";
            TokenText = $"Token: {dittoManager.DevelopmentToken}";

#if WINDOWS
                try
                {
                    Task.Run(() =>
                    {
                        ObserveDittoTasksCollection();
                        StartSync();

                    });
                }
                catch (Exception e)
                {
                    logger.LogError($"TasksPageviewModel: Unable to start Ditto sync: {e.Message}");
                }
#else

            DittoSyncPermissions.RequestPermissionsAsync().ContinueWith(t =>
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
#endif
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

                await tasksRepository.AddTask(title);
            }
            catch (Exception e)
            {
                logger.LogError($"TasksPageviewModel: Error adding task: {e.Message}");
            }
        }

        [RelayCommand]
        private async Task EditTaskAsync(TaskModel task)
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

                await tasksRepository.UpdateTaskTitle(task.Id, newTitle);
            }
            catch (Exception e)
            {
                logger.LogError($"TasksPageviewModel: Error editing task: {e.Message}");
            }
        }

        [RelayCommand]
        private void DeleteTask(TaskModel task)
        {
            try
            {
                _ = tasksRepository.DeleteTask(task.Id);
            }
            catch (Exception e)
            {
                logger.LogError($"TasksPageviewModel: Error deleting task: {e.Message}");
            }
        }

        [RelayCommand]
        private Task UpdateTaskDoneAsync(TaskModel task)
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
                        await tasksRepository.UpdateTaskDone(taskId, newDoneState);
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
            var observer = tasksRepository.ObserveTasksCollection(newTasks =>
            {
                MainThread.BeginInvokeOnMainThread(() =>
                {
                    try
                    {
                        if (Tasks == null)
                        {
                            Tasks = new ObservableCollection<TaskModel>(newTasks);
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

                return Task.CompletedTask;
            });

            // Hand the observer off (or, if Dispose() already ran, cancel the just-created
            // one so it isn't leaked) atomically with respect to Dispose().
            lock (observerGate)
            {
                if (!disposed)
                {
                    tasksObserver = observer;
                    return;
                }
            }

            observer.Cancel();
            observer.Dispose();
        }

        public void Dispose()
        {
            DittoStoreObserver observer;
            lock (observerGate)
            {
                if (disposed)
                {
                    return;
                }
                disposed = true;
                observer = tasksObserver;
                tasksObserver = null;
            }

            // Cancel and dispose the store observer so its Ditto callback is not leaked when
            // this view model goes away. The sync subscription is owned by the singleton
            // TasksRepository (registered in MauiProgram), not this view model, so it is
            // intentionally left alone here.
            observer?.Cancel();
            observer?.Dispose();
        }

        private void UpdateTasks(IList<TaskModel> newTasks)
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
                dittoManager.StartSync();
            }
            catch (Exception e)
            {
                logger.LogError($"TasksPageviewModel: Error starting Ditto sync: {e.Message}");
            }
        }

        private void StopSync()
        {
            try
            {
                dittoManager.StopSync();
            }
            catch (Exception e)
            {
                logger.LogError($"TasksPageviewModel: Error stopping Ditto sync: {e.Message}");
            }
        }
    }
}
