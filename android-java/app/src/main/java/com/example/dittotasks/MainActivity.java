package com.example.dittotasks;

import android.Manifest;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.ComponentActivity;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class MainActivity extends ComponentActivity {
    private TaskAdapter taskAdapter;
    private SwitchCompat syncSwitch;

    DittoManager dittoManager;
    TasksRepository tasksRepository;

    private final String dittoDatabaseId = BuildConfig.DITTO_DATABASE_ID;
    private final String dittoDevelopmentToken = BuildConfig.DITTO_DEVELOPMENT_TOKEN;
    private final String dittoServerUrl = BuildConfig.DITTO_SERVER_URL;
    private final String dittoOfflineLicenseToken = BuildConfig.DITTO_OFFLINE_LICENSE_TOKEN;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Keep screen on during testing to prevent NoActivityResumedException
        if(BuildConfig.DEBUG && isInstrumentationTest()){
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        
        initDitto();

        // Populate connection info (only in debug builds)
        if(BuildConfig.DEBUG) {
            TextView databaseId = findViewById(R.id.ditto_app_id);
            databaseId.setText(String.format("Database ID: %s", dittoDatabaseId));

            TextView developmentToken = findViewById(R.id.ditto_development_token);
            developmentToken.setText(String.format("Development Token: %s", dittoDevelopmentToken));
        } else {
            // Hide credential views in production
            findViewById(R.id.ditto_app_id).setVisibility(View.GONE);
            findViewById(R.id.ditto_development_token).setVisibility(View.GONE);
        }

        // Initialize "add task" fab
        FloatingActionButton addButton = findViewById(R.id.add_button);
        addButton.setOnClickListener(v -> showAddTaskModal());

        // Initialize sync switch
        syncSwitch = findViewById(R.id.sync_switch);
        syncSwitch.setChecked(true);
        syncSwitch.setOnCheckedChangeListener(((buttonView, isChecked) -> {
            toggleSync();
        }));

        // Initialize task list
        RecyclerView taskList = findViewById(R.id.task_list);
        taskList.setLayoutManager(new LinearLayoutManager(this));
        taskAdapter = new TaskAdapter();
        taskList.setAdapter(taskAdapter);
        taskAdapter.setOnTaskToggleListener((task, isChecked) -> {
            toggleTask(task);
        });
        taskAdapter.setOnTaskDeleteListener(this::deleteTask);
        taskAdapter.setOnTaskLongPressListener(this::showEditTaskModal);
        
        // Initialize empty list - Ditto observer will populate it
        taskAdapter.setTasks(Collections.emptyList());
    }


    void initDitto() {
        Log.d("DittoInit", "=== Starting Ditto initialization ===");

        Log.d("DittoInit", "DITTO_DATABASE_ID: " + dittoDatabaseId);
        Log.d("DittoInit", "DITTO_DEVELOPMENT_TOKEN: " + (dittoDevelopmentToken != null ? "Present" : "NULL"));
        Log.d("DittoInit", "DITTO_SERVER_URL: " + dittoServerUrl);

        // Skip permission requests during testing to avoid permission dialogs
        if (!isInstrumentationTest()) {
            Log.d("DittoInit", "Requesting permissions...");
            requestPermissions();
        } else {
            Log.d("DittoInit", "Skipping permissions during instrumentation test");
        }

        Log.d("DittoInit", "Starting Ditto SDK initialization...");
        try {
            // Create + configure the Ditto instance (identity/auth, sync lifecycle).
            Log.d("DittoInit", "Creating Ditto instance...");
            dittoManager = new DittoManager(
                    dittoDatabaseId,
                    dittoServerUrl,
                    dittoDevelopmentToken,
                    dittoOfflineLicenseToken
            );
            Log.d("DittoInit", "Ditto instance created successfully");

            // Set up the tasks concern: subscription + observer + CRUD. Register the
            // subscription, then start observing — the observer streams the visible
            // task list back to the UI.
            Log.d("DittoInit", "Setting up tasks repository...");
            tasksRepository = new TasksRepository(dittoManager);
            tasksRepository.registerSubscription();
            tasksRepository.observeTasks(tasks ->
                    runOnUiThread(() -> {
                        Log.d("DittoInit", "Updating UI with " + tasks.size() + " tasks");
                        taskAdapter.setTasks(new ArrayList<>(tasks));
                    }));
            Log.d("DittoInit", "Tasks repository ready");

            Log.d("DittoInit", "Starting Ditto sync...");
            dittoManager.startSync();
            Log.d("DittoInit", "=== Ditto initialization completed successfully ===");
        } catch (Exception e) {
            Log.e("DittoInit", "Error during Ditto initialization: " + e.getMessage(), e);
            e.printStackTrace();
        }
    }

    // Check if running under instrumentation (testing)
    private boolean isInstrumentationTest() {
        try {
            Class.forName("androidx.test.espresso.Espresso");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    // Request permissions for Ditto
    // https://docs.ditto.live/sdk/latest/install-guides/java#requesting-permissions-at-runtime
    void requestPermissions() {
        List<String> permissions = new ArrayList<>();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_ADVERTISE);
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT);
            permissions.add(Manifest.permission.BLUETOOTH_SCAN);
        }
        if (Build.VERSION.SDK_INT <= 32) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (Build.VERSION.SDK_INT <= 30) {
            permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.NEARBY_WIFI_DEVICES);
        }

        String[] missing = permissions.stream()
                .filter(p -> checkSelfPermission(p) != PackageManager.PERMISSION_GRANTED)
                .toArray(String[]::new);

        if (missing.length > 0) {
            this.requestPermissions(missing, 0);
        }
    }

    private void createTask(String title) {
        if (tasksRepository == null) {
            Log.i("MainActivity", "Ditto disabled - create task ignored: " + title);
            return;
        }
        tasksRepository.createTask(title);
    }

    private void editTaskTitle(Task task, String newTitle) {
        if (tasksRepository == null) {
            Log.i("MainActivity", "Ditto disabled - edit task ignored: " + task.getTitle());
            return;
        }
        tasksRepository.editTaskTitle(task, newTitle);
    }

    private void toggleTask(Task task) {
        if (tasksRepository == null) {
            Log.i("MainActivity", "Ditto disabled - toggle task ignored: " + task.getTitle());
            return;
        }
        tasksRepository.toggleTask(task);
    }

    private void deleteTask(Task task) {
        if (tasksRepository == null) {
            Log.i("MainActivity", "Ditto disabled - delete task ignored: " + task.getTitle());
            return;
        }
        tasksRepository.deleteTask(task);
    }

    private void toggleSync() {
        if (dittoManager == null) {
            return;
        }

        boolean isSyncActive = dittoManager.isSyncActive();
        var nextColor = isSyncActive ? null : ColorStateList.valueOf(0xFFBB86FC);
        var nextText = isSyncActive ? "Sync Inactive" : "Sync Active";

        // implement Ditto Sync
        // https://docs.ditto.live/sdk/latest/sync/start-and-stop-sync
        try {
            if (isSyncActive) {
                dittoManager.stopSync();
            } else {
                dittoManager.startSync();
            }
            syncSwitch.setChecked(!isSyncActive);
            syncSwitch.setTrackTintList(nextColor);
            syncSwitch.setText(nextText);
        } catch (Exception e) {
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
