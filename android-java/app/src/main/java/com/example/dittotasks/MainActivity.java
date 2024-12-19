package com.example.dittotasks;

import static com.example.dittotasks.DittoViewModel.DITTO_APP_ID;
import static com.example.dittotasks.DittoViewModel.DITTO_PLAYGROUND_TOKEN;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.ComponentActivity;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;
import java.util.Objects;

import live.ditto.transports.DittoSyncPermissions;

public class MainActivity extends ComponentActivity {
    DittoViewModel dittoViewModel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dittoViewModel = new ViewModelProvider(this).get(DittoViewModel.class);

        // Populate AppID view
        TextView appId = findViewById(R.id.ditto_app_id);
        appId.setText(String.format("App ID: %s", DITTO_APP_ID));

        // Populate Playground Token view
        TextView playgroundToken = findViewById(R.id.ditto_playground_token);
        playgroundToken.setText(String.format("Playground Token: %s", DITTO_PLAYGROUND_TOKEN));

        // Initialize "add task" fab
        FloatingActionButton addButton = findViewById(R.id.add_button);
        addButton.setOnClickListener(v -> showAddTaskModal());

        // Initialize sync switch
        SwitchCompat syncSwitch = findViewById(R.id.sync_switch);
        syncSwitch.setChecked(true);
        syncSwitch.setOnCheckedChangeListener(((buttonView, isChecked) -> {
            dittoViewModel.toggleSync();
        }));

        // Initialize task list
        RecyclerView taskList = findViewById(R.id.task_list);
        taskList.setLayoutManager(new LinearLayoutManager(this));
        TaskAdapter taskAdapter = dittoViewModel.getTaskAdapter();
        taskList.setAdapter(taskAdapter);
        taskAdapter.setOnTaskToggleListener((task, isChecked) -> {
            dittoViewModel.toggleTask(task);
        });
        taskAdapter.setOnTaskDeleteListener(dittoViewModel::deleteTask);
        taskAdapter.setOnTaskLongPressListener(this::showEditTaskModal);
        taskAdapter.setTasks(List.of());
    }

    // Request permissions for Ditto
    void requestPermissions() {
        DittoSyncPermissions permissions = new DittoSyncPermissions(this);
        String[] missing = permissions.missingPermissions(permissions.requiredPermissions());
        if (missing.length > 0) {
            this.requestPermissions(missing, 0);
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
                        dittoViewModel.createTask(text);
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
                        dittoViewModel.editTaskTitle(task, newTitle);
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
