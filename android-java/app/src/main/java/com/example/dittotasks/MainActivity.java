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

import com.ditto.kotlin.DittoStoreObserver;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class MainActivity extends ComponentActivity {
    private TaskAdapter taskAdapter;
    private SwitchCompat syncSwitch;

    // The store observer is scoped to this screen: registered in onCreate, closed in
    // onDestroy. The Ditto instance and the sync subscription are app-scoped singletons
    // (see DittoManager / TasksRepository) and are not owned here.
    private DittoStoreObserver tasksObserver;

    private final String dittoDatabaseId = BuildConfig.DITTO_DATABASE_ID;
    private final String dittoDevelopmentToken = BuildConfig.DITTO_DEVELOPMENT_TOKEN;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Keep screen on during testing to prevent NoActivityResumedException
        if(BuildConfig.DEBUG && isInstrumentationTest()){
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

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

        // Initialize sync switch from the real, app-scoped sync state (which survives
        // rotation). Set it before attaching the listener so this doesn't fire toggleSync().
        syncSwitch = findViewById(R.id.sync_switch);
        boolean syncActive = DittoManager.isSyncActive();
        syncSwitch.setChecked(syncActive);
        syncSwitch.setText(syncActive ? "Sync Active" : "Sync Inactive");
        syncSwitch.setTrackTintList(syncActive ? ColorStateList.valueOf(0xFFBB86FC) : null);
        syncSwitch.setOnCheckedChangeListener(((buttonView, isChecked) -> toggleSync()));

        // Initialize task list
        RecyclerView taskList = findViewById(R.id.task_list);
        taskList.setLayoutManager(new LinearLayoutManager(this));
        taskAdapter = new TaskAdapter();
        taskList.setAdapter(taskAdapter);
        taskAdapter.setOnTaskToggleListener((task, isChecked) -> toggleTask(task));
        taskAdapter.setOnTaskDeleteListener(this::deleteTask);
        taskAdapter.setOnTaskLongPressListener(this::showEditTaskModal);

        // Initialize empty list - the Ditto observer will populate it
        taskAdapter.setTasks(Collections.emptyList());

        // Wire up the tasks concern for this screen (subscription + observer).
        setUpTasks();
    }

    // Ditto itself is created and started once by TasksApplication; here we just wire up
    // the tasks concern for this screen: register the app-wide subscription (idempotent)
    // and start a store observer scoped to this Activity (closed in onDestroy).
    void setUpTasks() {
        // Skip permission requests during testing to avoid permission dialogs
        if (!isInstrumentationTest()) {
            requestPermissions();
        }

        TasksRepository.registerSubscription();
        tasksObserver = TasksRepository.observeTasks(tasks ->
                runOnUiThread(() -> taskAdapter.setTasks(new ArrayList<>(tasks))));
    }

    @Override
    protected void onDestroy() {
        // The store observer is tied to this screen's lifecycle — close it so it doesn't
        // leak across Activity recreation (e.g. rotation). The Ditto instance and the sync
        // subscription are app-scoped singletons and intentionally live on.
        if (tasksObserver != null) {
            tasksObserver.close();
            tasksObserver = null;
        }
        super.onDestroy();
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
        TasksRepository.createTask(title);
    }

    private void editTaskTitle(Task task, String newTitle) {
        TasksRepository.editTaskTitle(task, newTitle);
    }

    private void toggleTask(Task task) {
        TasksRepository.toggleTask(task);
    }

    private void deleteTask(Task task) {
        TasksRepository.deleteTask(task);
    }

    private void toggleSync() {
        boolean isSyncActive = DittoManager.isSyncActive();
        var nextColor = isSyncActive ? null : ColorStateList.valueOf(0xFFBB86FC);
        var nextText = isSyncActive ? "Sync Inactive" : "Sync Active";

        // implement Ditto Sync
        // https://docs.ditto.live/sdk/latest/sync/start-and-stop-sync
        try {
            if (isSyncActive) {
                DittoManager.stopSync();
            } else {
                DittoManager.startSync();
            }
            syncSwitch.setChecked(!isSyncActive);
            syncSwitch.setTrackTintList(nextColor);
            syncSwitch.setText(nextText);
        } catch (Exception e) {
            Log.e("MainActivity", "Failed to toggle sync", e);
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
