using System.Collections.ObjectModel;
using DittoMauiTasksApp.Data;
using DittoMauiTasksApp.Model;
using Microsoft.Extensions.Logging;
using Xunit;

namespace DittoMauiTasksApp.Tests
{
    /// <summary>
    /// Unit tests for the DittoManager class, testing CRUD operations and data synchronization.
    /// These tests verify that the DittoManager correctly handles task operations including
    /// insertion, updates, deletion, and state changes.
    /// </summary>
    public class DittoServiceTest
    {
 
        /// <summary>
        /// Tests that the InsertInitialTasks method correctly populates the database with
        /// the four predefined tasks: "Buy groceries", "Clean the kitchen", 
        /// "Schedule dentist appointment", and "Pay bills".
        /// </summary>
        /// <returns>A task that represents the asynchronous test operation.</returns>
        [Fact]
        public async Task InsertInitialTasks_ShouldAddFourTasks()
        {
            //arrange
            var manager = await InitializeDittoInstance();
            var tasks = new ObservableCollection<DittoTask>();
            
            //act
            manager.RegisterObservers(tasks);
            await Task.Delay(500); // Wait for observer to populate

            //assert
            Assert.Equal(4, tasks.Count);
            var titles = tasks.Select(t => t.Title).ToList();
            Assert.Contains("Buy groceries", titles);
            Assert.Contains("Clean the kitchen", titles);
            Assert.Contains("Schedule dentist appointment", titles);
            Assert.Contains("Pay bills", titles);
        }

        /// <summary>
        /// Tests that adding a new task via the AddTask method increases the total count
        /// of tasks in the collection and that the new task is properly added with
        /// the correct properties.
        /// </summary>
        /// <returns>A task that represents the asynchronous test operation.</returns>
        [Fact]
        public async Task AddTask_ShouldIncreaseCount()
        {
            //arrange
            var manager = await InitializeDittoInstance();
            var tasks = new ObservableCollection<DittoTask>();
            manager.RegisterObservers(tasks);
            await Task.Delay(500);
            var initialCount = tasks.Count;
            
            //act
            var newTask = CreateTestTask();
            var doc = new Dictionary<string, object>
            {
                { "_id", newTask.Id },
                { "title", newTask.Title },
                { "done", newTask.Done },
                { "deleted", newTask.Deleted }
            };
            await manager.AddTask(doc);
            await Task.Delay(500);

            //assert
            Assert.Equal(initialCount + 1, tasks.Count);
            Assert.Contains(tasks, t => t.Id == newTask.Id);
        }

        /// <summary>
        /// Tests that the EditTask method correctly updates the title of an existing task
        /// and that the change is reflected in the observable collection.
        /// </summary>
        /// <returns>A task that represents the asynchronous test operation.</returns>
        [Fact]
        public async Task EditTask_ShouldUpdateTitle()
        {
            //arrange
            var manager = await InitializeDittoInstance();
            var tasks = new ObservableCollection<DittoTask>();
            manager.RegisterObservers(tasks);
            await Task.Delay(500);

            //act
            var newTask = CreateTestTask();
            var doc = new Dictionary<string, object>
            {
                { "_id", newTask.Id },
                { "title", newTask.Title },
                { "done", newTask.Done },
                { "deleted", newTask.Deleted }
            };
            await manager.AddTask(doc);
            await Task.Delay(500);
            newTask.Title = "Updated Title";
            await manager.EditTask(newTask);
            await Task.Delay(500);

            //assert
            var updated = tasks.FirstOrDefault(t => t.Id == newTask.Id);
            Assert.NotNull(updated);
            Assert.Equal("Updated Title", updated.Title);
        }

        /// <summary>
        /// Tests that the DeleteTask method correctly marks a task as deleted using
        /// the soft delete pattern, removing it from the observable collection
        /// while keeping it in the database.
        /// </summary>
        /// <returns>A task that represents the asynchronous test operation.</returns>
        [Fact]
        public async Task DeleteTask_ShouldMarkAsDeleted()
        {
            //arrange
            var manager = await InitializeDittoInstance();
            var tasks = new ObservableCollection<DittoTask>();
            manager.RegisterObservers(tasks);
            await Task.Delay(500);

            //act
            var newTask = CreateTestTask();
            var doc = new Dictionary<string, object>
            {
                { "_id", newTask.Id },
                { "title", newTask.Title },
                { "done", newTask.Done },
                { "deleted", newTask.Deleted }
            };
            await manager.AddTask(doc);
            await Task.Delay(500);
            await manager.DeleteTask(newTask);
            await Task.Delay(500);
            
            //assert
            Assert.DoesNotContain(tasks, t => t.Id == newTask.Id);
        }

        /// <summary>
        /// Tests that the UpdateTaskDoneAsync method correctly updates the completion
        /// status of a task and that the change is reflected in the observable collection.
        /// </summary>
        /// <returns>A task that represents the asynchronous test operation.</returns>
        [Fact]
        public async Task UpdateTaskDoneAsync_ShouldToggleDone()
        {
            //arrange
            var manager = await InitializeDittoInstance();
            var tasks = new ObservableCollection<DittoTask>();
            manager.RegisterObservers(tasks);
            await Task.Delay(500);

            //act
            var newTask = CreateTestTask();
            var doc = new Dictionary<string, object>
            {
                { "_id", newTask.Id },
                { "title", newTask.Title },
                { "done", newTask.Done },
                { "deleted", newTask.Deleted }
            };
            await manager.AddTask(doc);
            await Task.Delay(500);
            newTask.Done = true;
            await manager.UpdateTaskDoneAsync(newTask);
            await Task.Delay(500);
            
            //assert
            var updated = tasks.FirstOrDefault(t => t.Id == newTask.Id);
            Assert.NotNull(updated);
            Assert.True(updated.Done);
        }
        
        /// <summary>
        /// Creates a DittoConfig object populated with test configuration values
        /// loaded from the embedded .env resource file.
        /// </summary>
        /// <returns>A DittoConfig object containing the test environment configuration.</returns>
        /// <exception cref="InvalidOperationException">Thrown when required environment variables are missing.</exception>
        private static DittoConfig GetTestConfig()
        {
            var envVars = LoadEnvVariables();
            return new DittoConfig
            {
                AppId = envVars["DITTO_APP_ID"],
                PlaygroundToken = envVars["DITTO_PLAYGROUND_TOKEN"],
                AuthUrl = envVars["DITTO_AUTH_URL"],
                WebsocketUrl = envVars["DITTO_WEBSOCKET_URL"]
            };
        }

        /// <summary>
        /// Loads environment variables from the embedded .env resource file.
        /// Parses the file line by line, skipping comments and empty lines,
        /// and extracts key-value pairs for configuration.
        /// </summary>
        /// <returns>A dictionary containing the environment variables loaded from the .env file.</returns>
        /// <exception cref="InvalidOperationException">Thrown when the .env resource file cannot be found or loaded.</exception>
        static Dictionary<string, string> LoadEnvVariables()
        {
            return DittoMauiTasksApp.MauiProgram.LoadEnvVariables();
        }

        /// <summary>
        /// Creates a new instance of DittoManager with a default logger for testing purposes.
        /// </summary>
        /// <returns>A new DittoManager instance configured with a test logger.</returns>
        private static DittoService CreateManager()
        {
            var logger = new LoggerFactory().CreateLogger<DittoService>();
            return new DittoService(logger);
        }

        /// <summary>
        /// Creates a test DittoTask instance with default values for testing purposes.
        /// </summary>
        /// <param name="id">Optional ID for the task. If not provided, a new GUID will be generated.</param>
        /// <returns>A new DittoTask instance with test data.</returns>
        private static DittoTask CreateTestTask(string? id = null)
        {
            return new DittoTask
            {
                Id = id ?? Guid.NewGuid().ToString(),
                Title = "Test Task",
                Done = false,
                Deleted = false
            };
        }

        /// <summary>
        /// Initializes a DittoManager instance with test configuration and a unique data path
        /// to ensure test isolation. Each test gets its own isolated Ditto instance.
        /// </summary>
        /// <returns>A fully initialized DittoManager instance ready for testing.</returns>
        /// <exception cref="Exception">Thrown when Ditto initialization fails.</exception>
        private static async Task<DittoService> InitializeDittoInstance()
        {
            var manager = CreateManager();
            var dataPath = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
                $"ditto-{Guid.NewGuid().ToString()}");
            await manager.Initialize(GetTestConfig(), dataPath);
            return manager;
        }
    }
}
