package com.ditto.example.spring.quickstart.controller;

import com.ditto.example.spring.quickstart.service.DittoTaskService;
import com.ditto.example.spring.quickstart.service.Task;
import jakarta.annotation.Nonnull;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Controller
public class TaskContentController {
    @Nonnull
    private final DittoTaskService taskService;

    public TaskContentController(@NotNull final DittoTaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping("/")
    public String index(Map<String, Object> model) {
        List<Task> tasks = taskService.observeAll().blockFirst();
        model.put("tasks",  tasks != null ? tasks : Collections.emptyList());
        return "index";
    }

    @GetMapping("/tasks/{id}/edit-form")
    public String editForm(@PathVariable String id, Model model) {
        model.addAttribute("id", id);
        return "fragments/editForm :: editFormFrag";
    }
}
