package com.ditto.example.spring.quickstart.service;

import jakarta.annotation.Nonnull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "ditto.enabled", havingValue = "false")
public class MockDittoTaskService extends DittoTaskService {
    
    private final ConcurrentMap<String, Task> tasks = new ConcurrentHashMap<>();

    // MockDittoTaskService doesn't depend on DittoService, so we pass null
    public MockDittoTaskService() {
        super(null);
        // Add some sample data for testing
        addTask("Sample Task 1");
        addTask("Sample Task 2");
        addTask("Complete BrowserStack Integration");
    }

    @Override
    public void addTask(@Nonnull String title) {
        String taskId = UUID.randomUUID().toString();
        tasks.put(taskId, new Task(taskId, title, false, false));
    }

    @Override
    public void toggleTaskDone(@Nonnull String taskId) {
        Task task = tasks.get(taskId);
        if (task != null) {
            tasks.put(taskId, new Task(task.id(), task.title(), !task.done(), task.deleted()));
        }
    }

    @Override
    public void deleteTask(@Nonnull String taskId) {
        Task task = tasks.get(taskId);
        if (task != null) {
            tasks.put(taskId, new Task(task.id(), task.title(), task.done(), true));
        }
    }

    @Override
    public void updateTask(@Nonnull String taskId, @Nonnull String newTitle) {
        Task task = tasks.get(taskId);
        if (task != null) {
            tasks.put(taskId, new Task(task.id(), newTitle, task.done(), task.deleted()));
        }
    }

    @Override
    @Nonnull
    public Flux<List<Task>> observeAll() {
        List<Task> activeTasks = tasks.values().stream()
                .filter(task -> !task.deleted())
                .sorted((a, b) -> a.id().compareTo(b.id()))
                .toList();
        return Flux.just(activeTasks);
    }
}