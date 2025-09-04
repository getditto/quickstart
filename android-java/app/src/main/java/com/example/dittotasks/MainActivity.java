package com.example.dittotasks;

import android.app.AlertDialog;
import android.content.res.ColorStateList;
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
import live.ditto.DittoSyncSubscription;
import live.ditto.android.DefaultAndroidDittoDependencies;
import live.ditto.transports.DittoSyncPermissions;
import live.ditto.transports.DittoTransportConfig;
// import live.ditto.Logger; // Import not found, will try alternative

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

    // This is required to be set to false to use the correct URLs
    private Boolean DITTO_ENABLE_CLOUD_SYNC = true;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Keep screen on during testing to prevent NoActivityResumedException
        if(BuildConfig.DEBUG){
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        
        initDitto(); // Re-enabled with debug logging

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

    private void addTestTasks() {
        // Add some test tasks including our target task for testing
        List<Task> testTasks = List.of(
            new Task("1", "Learn Android Testing", false, false),
            new Task("2", "Basic Test Task", false, false), // This is our target task
            new Task("3", "Setup CI Pipeline", false, false),
            new Task("4", "Write Documentation", true, false),
            new Task("5", "Review Code Changes", false, false)
        );
        taskAdapter.setTasks(testTasks);
        Log.i("MainActivity", "Added " + testTasks.size() + " test tasks including 'Basic Test Task'");
    }

    void initDitto() {
        Log.d("DittoInit", "=== Starting Ditto initialization ===");
        
        // Enable Ditto's internal debug logging (if available)
        Log.d("DittoInit", "Ditto Logger class not available in this version, using Android Log instead");
        
        Log.d("DittoInit", "DITTO_APP_ID: " + DITTO_APP_ID);
        Log.d("DittoInit", "DITTO_PLAYGROUND_TOKEN: " + (DITTO_PLAYGROUND_TOKEN != null ? "Present" : "NULL"));
        Log.d("DittoInit", "DITTO_AUTH_URL: " + DITTO_AUTH_URL);
        Log.d("DittoInit", "DITTO_WEBSOCKET_URL: " + DITTO_WEBSOCKET_URL);
        Log.d("DittoInit", "DITTO_ENABLE_CLOUD_SYNC: " + DITTO_ENABLE_CLOUD_SYNC);
        
        // Skip permission requests during testing to avoid permission dialogs
        if (!isInstrumentationTest()) {
            Log.d("DittoInit", "Requesting permissions...");
            requestPermissions();
        } else {
            Log.d("DittoInit", "Skipping permissions during instrumentation test");
        }

        Log.d("DittoInit", "Starting Ditto SDK initialization...");
        try {
            Log.d("DittoInit", "Creating AndroidDependencies...");
            DittoDependencies androidDependencies = new DefaultAndroidDittoDependencies(getApplicationContext());
            Log.d("DittoInit", "AndroidDependencies created successfully");
            
            /*
             *  Setup Ditto Identity
             *  https://docs.ditto.live/sdk/latest/install-guides/java#integrating-and-initializing
             */
            Log.d("DittoInit", "Creating DittoIdentity.OnlinePlayground...");
            var identity = new DittoIdentity
                    .OnlinePlayground(
                            androidDependencies,
                            DITTO_APP_ID,
                            DITTO_PLAYGROUND_TOKEN,
                            DITTO_ENABLE_CLOUD_SYNC, // This is required to be set to false to use the correct URLs
                            DITTO_AUTH_URL);
            Log.d("DittoInit", "DittoIdentity created successfully");
            
            Log.d("DittoInit", "Creating Ditto instance...");
            ditto = new Ditto(androidDependencies, identity);
            Log.d("DittoInit", "Ditto instance created successfully");

            //https://docs.ditto.live/sdk/latest/sync/customizing-transport-configurations
            Log.d("DittoInit", "Updating transport config...");
            ditto.updateTransportConfig(config -> {
                config.getConnect().getWebsocketUrls().add(DITTO_WEBSOCKET_URL);

                // lambda must return Kotlin Unit which corresponds to 'void' in Java
                return kotlin.Unit.INSTANCE;
            });
            Log.d("DittoInit", "Transport config updated");

            // disable sync with v3 peers, required for DQL
            Log.d("DittoInit", "Disabling sync with v3...");
            ditto.disableSyncWithV3();
            Log.d("DittoInit", "Sync with v3 disabled");

            // Disable DQL strict mode
            // when set to false, collection definitions are no longer required. SELECT queries will return and display all fields by default.
            // https://docs.ditto.live/dql/strict-mode
            Log.d("DittoInit", "Setting DQL strict mode to false...");
            ditto.store.execute("ALTER SYSTEM SET DQL_STRICT_MODE = false");
            Log.d("DittoInit", "DQL strict mode disabled");

            // register subscription
            // https://docs.ditto.live/sdk/latest/sync/syncing-data#creating-subscriptions
            Log.d("DittoInit", "Registering subscription...");
            taskSubscription = ditto.sync.registerSubscription("SELECT * FROM tasks");
            Log.d("DittoInit", "Subscription registered");

            // register observer for live query
            // https://docs.ditto.live/sdk/latest/crud/observing-data-changes#setting-up-store-observers
            Log.d("DittoInit", "Registering observer...");
            taskObserver = ditto.store.registerObserver("SELECT * FROM tasks WHERE deleted=false ORDER BY title ASC", null, result -> {
                Log.d("DittoInit", "Observer callback triggered with " + result.getItems().size() + " items");
                var tasks = result.getItems().stream().map(Task::fromQueryItem).collect(Collectors.toCollection(ArrayList::new));
                runOnUiThread(() -> {
                    Log.d("DittoInit", "Updating UI with " + tasks.size() + " tasks");
                    taskAdapter.setTasks(new ArrayList<>(tasks));
                });
                return Unit.INSTANCE;
            });
            Log.d("DittoInit", "Observer registered");

            Log.d("DittoInit", "Starting Ditto sync...");
            ditto.startSync();
            Log.d("DittoInit", "=== Ditto initialization completed successfully ===");
        } catch (DittoError e) {
            Log.e("DittoInit", "DittoError during initialization: " + e.getMessage(), e);
            e.printStackTrace();
        } catch (Exception e) {
            Log.e("DittoInit", "Unexpected error during Ditto initialization: " + e.getMessage(), e);
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

            // Add tasks into the ditto collection using DQL INSERT statement
            // https://docs.ditto.live/sdk/latest/crud/write#inserting-documents
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
            // Update tasks into the ditto collection using DQL UPDATE statement
            // https://docs.ditto.live/sdk/latest/crud/update#updating
            ditto.store.execute("UPDATE tasks SET title=:title WHERE _id=:id", args);
        } catch (DittoError e) {
            e.printStackTrace();
        }
    }

    private void toggleTask(Task task) {
        if (ditto == null) {
            Log.i("MainActivity", "Ditto disabled - toggle task ignored: " + task.getTitle());
            return;
        }
        
        HashMap<String, Object> args = new HashMap<>();
        args.put("id", task.getId());
        args.put("done", !task.isDone());

        try {
            // Update tasks into the ditto collection using DQL UPDATE statement
            // https://docs.ditto.live/sdk/latest/crud/update#updating
            ditto.store.execute("UPDATE tasks SET done=:done WHERE _id=:id", args);
        } catch (DittoError e) {
            e.printStackTrace();
        }
    }

    private void deleteTask(Task task) {
        if (ditto == null) {
            Log.i("MainActivity", "Ditto disabled - delete task ignored: " + task.getTitle());
            return;
        }
        
        HashMap<String, Object> args = new HashMap<>();
        args.put("id", task.getId());
        try {
            // UPDATE DQL Statement using Soft-Delete pattern
            // https://docs.ditto.live/sdk/latest/crud/delete#soft-delete-pattern
            ditto.store.execute("UPDATE tasks SET deleted=true WHERE _id=:id", args);
        } catch (DittoError e) {
            e.printStackTrace();
        }
    }

    private void toggleSync() {
        if (ditto == null) {
            return;
        }

        boolean isSyncActive = ditto.isSyncActive();
        var nextColor = isSyncActive ? null : ColorStateList.valueOf(0xFFBB86FC);
        var nextText = isSyncActive ? "Sync Inactive" : "Sync Active";

        // implement Ditto Sync
        // https://docs.ditto.live/sdk/latest/sync/start-and-stop-sync
        try {
            if (isSyncActive) {
                ditto.stopSync();
            } else {
                ditto.startSync();
            }
            syncSwitch.setChecked(!isSyncActive);
            syncSwitch.setTrackTintList(nextColor);
            syncSwitch.setText(nextText);
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
