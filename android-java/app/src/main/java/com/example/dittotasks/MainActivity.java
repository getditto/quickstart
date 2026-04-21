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

import com.ditto.kotlin.Ditto;
import com.ditto.kotlin.DittoStoreObserver;
import com.ditto.kotlin.DittoSyncSubscription;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class MainActivity extends ComponentActivity {
    private TaskAdapter taskAdapter;
    private SwitchCompat syncSwitch;

    Ditto ditto;
    DittoSyncSubscription taskSubscription;
    DittoStoreObserver taskObserver;

    private String DITTO_APP_ID = BuildConfig.DITTO_APP_ID;
    private String DITTO_PLAYGROUND_TOKEN = BuildConfig.DITTO_PLAYGROUND_TOKEN;
    private String DITTO_AUTH_URL = BuildConfig.DITTO_AUTH_URL;
    private String DITTO_WEBSOCKET_URL = BuildConfig.DITTO_WEBSOCKET_URL;

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
            TextView appId = findViewById(R.id.ditto_app_id);
            appId.setText(String.format("App ID: %s", DITTO_APP_ID));

            TextView playgroundToken = findViewById(R.id.ditto_playground_token);
            playgroundToken.setText(String.format("Playground Token: %s", DITTO_PLAYGROUND_TOKEN));
        } else {
            // Hide credential views in production
            findViewById(R.id.ditto_app_id).setVisibility(View.GONE);
            findViewById(R.id.ditto_playground_token).setVisibility(View.GONE);
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
        taskAdapter.setTasks(List.of());
    }


    void initDitto() {
        Log.d("DittoInit", "=== Starting Ditto initialization ===");

        Log.d("DittoInit", "DITTO_APP_ID: " + DITTO_APP_ID);
        Log.d("DittoInit", "DITTO_PLAYGROUND_TOKEN: " + (DITTO_PLAYGROUND_TOKEN != null ? "Present" : "NULL"));
        Log.d("DittoInit", "DITTO_AUTH_URL: " + DITTO_AUTH_URL);

        // Skip permission requests during testing to avoid permission dialogs
        if (!isInstrumentationTest()) {
            Log.d("DittoInit", "Requesting permissions...");
            requestPermissions();
        } else {
            Log.d("DittoInit", "Skipping permissions during instrumentation test");
        }

        Log.d("DittoInit", "Starting Ditto SDK initialization...");
        try {
            // Create Ditto with server connection
            // https://docs.ditto.live/sdk/latest/install-guides/java#integrating-and-initializing
            Log.d("DittoInit", "Creating Ditto instance...");
            ditto = DittoHelper.createDitto(DITTO_APP_ID, DITTO_AUTH_URL);
            Log.d("DittoInit", "Ditto instance created successfully");

            // Set up authentication handler (must be set before sync.start())
            Log.d("DittoInit", "Setting up authentication...");
            DittoHelper.setupAuth(ditto, DITTO_PLAYGROUND_TOKEN);
            Log.d("DittoInit", "Authentication configured");

            // register subscription
            // https://docs.ditto.live/sdk/latest/sync/syncing-data#creating-subscriptions
            Log.d("DittoInit", "Registering subscription...");
            taskSubscription = DittoHelper.registerSubscription(ditto, "SELECT * FROM tasks");
            Log.d("DittoInit", "Subscription registered");

            // register observer for live query
            // https://docs.ditto.live/sdk/latest/crud/observing-data-changes#setting-up-store-observers
            Log.d("DittoInit", "Registering observer...");
            taskObserver = DittoHelper.registerObserver(ditto,
                    "SELECT * FROM tasks WHERE deleted=false ORDER BY title ASC",
                    result -> {
                        Log.d("DittoInit", "Observer callback triggered with " + result.getItems().size() + " items");
                        var tasks = result.getItems().stream().map(Task::fromQueryItem).collect(Collectors.toCollection(ArrayList::new));
                        runOnUiThread(() -> {
                            Log.d("DittoInit", "Updating UI with " + tasks.size() + " tasks");
                            taskAdapter.setTasks(new ArrayList<>(tasks));
                        });
                    });
            Log.d("DittoInit", "Observer registered");

            Log.d("DittoInit", "Starting Ditto sync...");
            DittoHelper.startSync(ditto);
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
        HashMap<String, Object> task = new HashMap<>();
        task.put("title", title);
        task.put("done", false);
        task.put("deleted", false);

        Map<String, Object> args = Map.of("task", task);

        try {
            // Add tasks into the ditto collection using DQL INSERT statement
            // https://docs.ditto.live/sdk/latest/crud/write#inserting-documents
            DittoHelper.execute(ditto, "INSERT INTO tasks DOCUMENTS (:task)", args);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void editTaskTitle(Task task, String newTitle) {
        Map<String, Object> args = Map.of("id", task.getId(), "title", newTitle);

        try {
            // Update tasks into the ditto collection using DQL UPDATE statement
            // https://docs.ditto.live/sdk/latest/crud/update#updating
            DittoHelper.execute(ditto, "UPDATE tasks SET title=:title WHERE _id=:id", args);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void toggleTask(Task task) {
        if (ditto == null) {
            Log.i("MainActivity", "Ditto disabled - toggle task ignored: " + task.getTitle());
            return;
        }

        Map<String, Object> args = Map.of("id", task.getId(), "done", !task.isDone());

        try {
            // Update tasks into the ditto collection using DQL UPDATE statement
            // https://docs.ditto.live/sdk/latest/crud/update#updating
            DittoHelper.execute(ditto, "UPDATE tasks SET done=:done WHERE _id=:id", args);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void deleteTask(Task task) {
        if (ditto == null) {
            Log.i("MainActivity", "Ditto disabled - delete task ignored: " + task.getTitle());
            return;
        }

        Map<String, Object> args = Map.of("id", task.getId());

        try {
            // UPDATE DQL Statement using Soft-Delete pattern
            // https://docs.ditto.live/sdk/latest/crud/delete#soft-delete-pattern
            DittoHelper.execute(ditto, "UPDATE tasks SET deleted=true WHERE _id=:id", args);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void toggleSync() {
        if (ditto == null) {
            return;
        }

        boolean isSyncActive = DittoHelper.isSyncActive(ditto);
        var nextColor = isSyncActive ? null : ColorStateList.valueOf(0xFFBB86FC);
        var nextText = isSyncActive ? "Sync Inactive" : "Sync Active";

        // implement Ditto Sync
        // https://docs.ditto.live/sdk/latest/sync/start-and-stop-sync
        try {
            if (isSyncActive) {
                DittoHelper.stopSync(ditto);
            } else {
                DittoHelper.startSync(ditto);
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
