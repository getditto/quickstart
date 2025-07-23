using System.Collections.ObjectModel;
using DittoMauiTasksApp.Model;

namespace DittoMauiTasksApp.Data;

public interface IDataService
{
    Task Initialize(DittoConfig dittoConfig);
    void RegisterObservers(ObservableCollection<DittoTask> tasks);
    void StartSync();
    void StopSync(); 
    
    Task InsertInitialTasks();
    Task AddTask(Dictionary<string, object> document);
    Task EditTask(DittoTask task);
    Task DeleteTask(DittoTask task);
    Task UpdateTaskDoneAsync(DittoTask task);

}