package com.example.dittotasks;

import android.app.AlertDialog;
import android.content.res.ColorStateList;
import android.os.Bundle;
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
    private Boolean DITTO_ENABLE_CLOUD_SYNC = false;

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
        taskAdapter.setTasks(List.of());
    }

    void initDitto() {
        requestPermissions();

        try {
            DittoDependencies androidDependencies = new DefaultAndroidDittoDependencies(getApplicationContext());
            /*
             *  Setup Ditto Identity
             *  https://docs.ditto.live/sdk/latest/install-guides/java#integrating-and-initializing
             */
            var identity = new DittoIdentity
                    .OnlinePlayground(
                            androidDependencies,
                            DITTO_APP_ID,
                            DITTO_PLAYGROUND_TOKEN,
                            DITTO_ENABLE_CLOUD_SYNC, // This is required to be set to false to use the correct URLs
                            DITTO_AUTH_URL);
            ditto = new Ditto(androidDependencies, identity);

            ditto.updateTransportConfig(config -> {
                config.getConnect().getWebsocketUrls().add(DITTO_WEBSOCKET_URL);

                // Enable all P2P transports
                config.enableAllPeerToPeer();

                return null;
            });

            // disable sync with v3 peers, required for DQL
            ditto.disableSyncWithV3();

            // register subscription
            // https://docs.ditto.live/sdk/latest/sync/syncing-data#creating-subscriptions
            taskSubscription = ditto.sync.registerSubscription("SELECT * FROM tasks");

            // register observer for live query
            // https://docs.ditto.live/sdk/latest/crud/observing-data-changes#setting-up-store-observers
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
