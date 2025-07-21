using System.Collections.ObjectModel;
using System.Reflection;
using DittoMauiTasksApp.Data;
using DittoMauiTasksApp.Model;
using Microsoft.Extensions.Logging;
using Xunit;

namespace DittoMauiTasksApp.Tests
{
    public class DittoManagerTest
    {
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
        /// Load environment variables from the embedded .env resource file.
        /// </summary>
        static Dictionary<string, string> LoadEnvVariables()
        {
            var envVars = new Dictionary<string, string>();
            var assembly = Assembly.GetExecutingAssembly();
            const string resourceName = @"DittoMauiTasksApp.Tests.env";

            using var stream = assembly.GetManifestResourceStream(resourceName);
            if (stream == null)
            {
                var availableResources = string.Join(Environment.NewLine, assembly.GetManifestResourceNames());
                throw new InvalidOperationException($"Resource '{resourceName}' not found. Available resources: {availableResources}");
            }

            using var reader = new StreamReader(stream);
            while (reader.ReadLine() is { } line)
            {
                line = line.Trim();

                if (string.IsNullOrEmpty(line) || line.StartsWith("#"))
                {
                    continue;
                }

                var separatorIndex = line.IndexOf('=');
                if (separatorIndex < 0)
                {
                    continue;
                }

                var key = line.Substring(0, separatorIndex).Trim();
                var value = line.Substring(separatorIndex + 1).Trim();

                if (value.StartsWith(@"\") && value.EndsWith(@"\") && value.Length >= 2)
                {
                    value = value.Substring(1, value.Length - 2);
                }

                envVars[key] = value;
            }

            return envVars;
        }

        private static DittoManager CreateManager()
        {
            var logger = new LoggerFactory().CreateLogger<DittoManager>();
            return new DittoManager(logger);
        }

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

        [Fact]
        public async Task InsertInitialTasks_ShouldAddFourTasks()
        {
            var manager = CreateManager();
            await manager.Initialize(GetTestConfig());
            var tasks = new ObservableCollection<DittoTask>();
            manager.RegisterObservers(tasks);

            await Task.Delay(500); // Wait for observer to populate

            Assert.Equal(4, tasks.Count);
            var titles = tasks.Select(t => t.Title).ToList();
            Assert.Contains("Buy groceries", titles);
            Assert.Contains("Clean the kitchen", titles);
            Assert.Contains("Schedule dentist appointment", titles);
            Assert.Contains("Pay bills", titles);
        }

        [Fact]
        public async Task AddTask_ShouldIncreaseCount()
        {
            var manager = CreateManager();
            await manager.Initialize(GetTestConfig());
            var tasks = new ObservableCollection<DittoTask>();
            manager.RegisterObservers(tasks);

            await Task.Delay(500);

            var initialCount = tasks.Count;
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

            Assert.Equal(initialCount + 1, tasks.Count);
            Assert.Contains(tasks, t => t.Id == newTask.Id);
        }

        [Fact]
        public async Task EditTask_ShouldUpdateTitle()
        {
            var manager = CreateManager();
            await manager.Initialize(GetTestConfig());
            var tasks = new ObservableCollection<DittoTask>();
            manager.RegisterObservers(tasks);

            await Task.Delay(500);

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

            var updated = tasks.FirstOrDefault(t => t.Id == newTask.Id);
            Assert.NotNull(updated);
            Assert.Equal("Updated Title", updated.Title);
        }

        [Fact]
        public async Task DeleteTask_ShouldMarkAsDeleted()
        {
            var manager = CreateManager();
            await manager.Initialize(GetTestConfig());
            var tasks = new ObservableCollection<DittoTask>();
            manager.RegisterObservers(tasks);

            await Task.Delay(500);

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

            Assert.DoesNotContain(tasks, t => t.Id == newTask.Id);
        }

        [Fact]
        public async Task UpdateTaskDoneAsync_ShouldToggleDone()
        {
            var manager = CreateManager();
            await manager.Initialize(GetTestConfig());
            var tasks = new ObservableCollection<DittoTask>();
            manager.RegisterObservers(tasks);

            await Task.Delay(500);

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

            var updated = tasks.FirstOrDefault(t => t.Id == newTask.Id);
            Assert.NotNull(updated);
            Assert.True(updated.Done);
        }
    }
}
