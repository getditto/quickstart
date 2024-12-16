package com.example.dittotasks;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.ComponentActivity;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import kotlin.Unit;
import live.ditto.Ditto;
import live.ditto.DittoDependencies;
import live.ditto.DittoError;
import live.ditto.DittoIdentity;
import live.ditto.DittoStoreObserver;
import live.ditto.android.DefaultAndroidDittoDependencies;
import live.ditto.transports.DittoSyncPermissions;

public class MainActivity extends ComponentActivity {
    private TaskAdapter taskAdapter;

    Ditto ditto;
    live.ditto.DittoSyncSubscription taskSubscription;
    DittoStoreObserver taskObserver;

    private String DITTO_APP_ID = "";
    private String DITTO_PLAYGROUND_TOKEN = "";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initDitto();

        // Populate AppID view
        TextView appId = findViewById(R.id.ditto_app_id);
        appId.setText(String.format("App ID: %s", DITTO_APP_ID));

        // Populate Playground Token view
        TextView playgroundToken = findViewById(R.id.ditto_playground_token);
        playgroundToken.setText(String.format("Playground Token: %s", DITTO_PLAYGROUND_TOKEN));

        // Initialize "add task" fab
        FloatingActionButton addButton = findViewById(R.id.add_button);
        addButton.setOnClickListener(v -> showAddTaskModal());

        // Initialize task list
        RecyclerView taskList = findViewById(R.id.task_list);
        taskList.setLayoutManager(new LinearLayoutManager(this));
        taskAdapter = new TaskAdapter();
        taskList.setAdapter(taskAdapter);
        taskAdapter.setOnTaskClickListener((task, isChecked) -> {
            toggleTask(task);
        });
        taskAdapter.setOnTaskDeleteListener(this::deleteTask);
        taskAdapter.setOnTaskLongPressListener(this::showEditTaskModal);
        taskAdapter.setTasks(List.of());
    }

    void initDitto() {
        requestPermissions();

        try {
            DittoDependencies androidDependencies = new DefaultAndroidDittoDependencies(getApplicationContext());
            var identity = new DittoIdentity.OnlinePlayground(androidDependencies, DITTO_APP_ID, DITTO_PLAYGROUND_TOKEN);
            ditto = new Ditto(androidDependencies, identity);
            ditto.disableSyncWithV3();

            taskSubscription = ditto.sync.registerSubscription("SELECT * FROM tasks");
            taskObserver = ditto.store.registerObserver("SELECT * FROM tasks WHERE deleted=false ORDER BY _id", null, result -> {
                var tasks = result.getItems().stream().map(Task::fromQueryItem).collect(Collectors.toCollection(ArrayList::new));
                runOnUiThread(() -> {
                    taskAdapter.setTasks(new ArrayList<>(tasks));
                });
                return Unit.INSTANCE;
            });

            ditto.startSync();
        } catch (DittoError e) {
            e.printStackTrace();
        }
    }

    // Request permissions for Ditto
    void requestPermissions() {
        DittoSyncPermissions permissions = new DittoSyncPermissions(this);
        String[] missing = permissions.missingPermissions(permissions.requiredPermissions());
        if (missing.length > 0) {
            this.requestPermissions(missing, 0);
        }
    }

    private void createTask(String title) {
        HashMap<String, Object> task = new HashMap<>();
        task.put("title", title);
        task.put("done", false);
        task.put("deleted", false);

        HashMap<String, Object> args = new HashMap<>();
        args.put("task", task);
        try {
            ditto.store.execute("INSERT INTO tasks DOCUMENTS (:task)", args);
        } catch (DittoError e) {
            e.printStackTrace();
        }
    }

    private void editTaskTitle(Task task, String newTitle) {
        HashMap<String, Object> args = new HashMap<>();
        args.put("id", task.getId());
        args.put("title", newTitle);

        try {
            ditto.store.execute("UPDATE tasks SET title=:title WHERE _id=:id", args);
        } catch (DittoError e) {
            e.printStackTrace();
        }
    }

    private void toggleTask(Task task) {
        HashMap<String, Object> args = new HashMap<>();
        args.put("id", task.getId());
        args.put("done", !task.isDone());

        try {
            ditto.store.execute("UPDATE tasks SET done=:done WHERE _id=:id", args);
        } catch (DittoError e) {
            e.printStackTrace();
        }
    }

    private void deleteTask(Task task) {
        HashMap<String, Object> args = new HashMap<>();
        args.put("id", task.getId());
        try {
            ditto.store.execute("UPDATE tasks SET deleted=true WHERE _id=:id", args);
        } catch (DittoError e) {
            e.printStackTrace();
        }
    }

    private void showAddTaskModal() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.modal_new_task, null);
        EditText modalTaskTitle = dialogView.findViewById(R.id.modal_task_title);

        builder.setView(dialogView)
                .setPositiveButton("Add", (dialog, which) -> {
                    String text = modalTaskTitle.getText().toString().trim();
                    if (!text.isEmpty()) {
                        createTask(text);
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        AlertDialog dialog = builder.create();
        dialog.show();

        // Show keyboard automatically
        modalTaskTitle.requestFocus();
        Objects.requireNonNull(dialog.getWindow()).setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
    }

    private void showEditTaskModal(Task task) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.modal_edit_task, null);
        EditText modalEditTaskTitle = dialogView.findViewById(R.id.modal_edit_task_title);

        // Pre-fill the current task title
        modalEditTaskTitle.setText(task.getTitle());
        modalEditTaskTitle.setSelection(task.getTitle().length()); // Place cursor at end

        builder.setView(dialogView)
                .setTitle("Edit Task")
                .setPositiveButton("Save", (dialog, which) -> {
                    String newTitle = modalEditTaskTitle.getText().toString().trim();
                    if (!newTitle.isEmpty()) {
                        editTaskTitle(task, newTitle);
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        AlertDialog dialog = builder.create();
        dialog.show();

        // Show keyboard automatically
        modalEditTaskTitle.requestFocus();
        Objects.requireNonNull(dialog.getWindow())
                .setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
    }
}
