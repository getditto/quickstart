using System.Collections.ObjectModel;
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
            return MauiProgram.GetDittoConfig();
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
