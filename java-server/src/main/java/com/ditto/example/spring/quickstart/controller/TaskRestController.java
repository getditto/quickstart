package com.ditto.example.spring.quickstart.controller;

import com.ditto.example.spring.quickstart.service.TasksRepository;
import com.ditto.example.spring.quickstart.service.Task;
import jakarta.annotation.Nonnull;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Set;

@RestController
public class TaskRestController {
    @Nonnull
    private final TasksRepository tasksRepository;
    @Nonnull
    private final SpringTemplateEngine templateEngine;

    public TaskRestController(final TasksRepository tasksRepository, final SpringTemplateEngine templateEngine) {
        this.tasksRepository = tasksRepository;
        this.templateEngine = templateEngine;
    }

    @GetMapping(value = "/tasks/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamTasks() {
        return tasksRepository.observeAll().map(tasks -> {
            String htmlFragment = renderTodoList(tasks);
            return ServerSentEvent.builder(htmlFragment)
                    .event("task_list")
                    .build();
        });
    }

    @PostMapping("/tasks")
    public String addTask(@RequestParam("title") @Nonnull String title) {
        tasksRepository.addTask(title);
        return "";
    }

    @PostMapping("/tasks/{taskId}/toggle")
    public String toggleTaskDone(@PathVariable @Nonnull String taskId, @RequestParam boolean done) {
        tasksRepository.setTaskDone(taskId, done);
        return "";
    }

    @DeleteMapping("/tasks/{taskId}")
    public String deleteTask(@PathVariable @Nonnull String taskId) {
        tasksRepository.deleteTask(taskId);
        return "";
    }

    @PutMapping("/tasks/{taskId}")
    public String updateTask(@PathVariable @Nonnull String taskId, @RequestParam @Nonnull String title) {
        tasksRepository.updateTask(taskId, title);
        return "";
    }


    @Nonnull
    private String renderTodoList(@Nonnull List<Task> tasks) {
        Context context = new Context();
        context.setVariable("tasks", tasks);
        return templateEngine.process("fragments/taskList", Set.of("taskListFrag"), context);
    }
}
