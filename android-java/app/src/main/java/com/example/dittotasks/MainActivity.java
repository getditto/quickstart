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
    private ArrayList<Task> tasks = new ArrayList<>();
    private TodoAdapter todoAdapter;

    Ditto ditto;
    live.ditto.DittoSyncSubscription taskSubscription;
    DittoStoreObserver taskObserver;

    private String DITTO_APP_ID = "";
    private String DITTO_PLAYGROUND_TOKEN = "";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        requestPermissions();

        try {
            DittoDependencies androidDependencies = new DefaultAndroidDittoDependencies(getApplicationContext());
            var identity = new DittoIdentity.OnlinePlayground(androidDependencies, DITTO_APP_ID, DITTO_PLAYGROUND_TOKEN);
            ditto = new Ditto(androidDependencies, identity);
            ditto.disableSyncWithV3();

            taskSubscription = ditto.sync.registerSubscription("SELECT * FROM tasks");
            taskObserver = ditto.store.registerObserver("SELECT * FROM tasks WHERE deleted=false ORDER BY _id", null, result -> {
                tasks = result.getItems().stream().map(Task::fromQueryItem).collect(Collectors.toCollection(ArrayList::new));
                System.out.printf("Observed tasks: %s", tasks);
                updateTodoList();
                return Unit.INSTANCE;
            });

            ditto.startSync();
        } catch (DittoError e) {
            e.printStackTrace();
        }

        tasks.add(new Task("Gardening"));
        tasks.add(new Task("Kitchening"));
        tasks.add(new Task("Cleaning"));
        tasks.add(new Task("Sleeping"));

        TextView appId = findViewById(R.id.ditto_app_id);
        appId.setText(String.format("App ID: %s", DITTO_APP_ID));

        TextView playgroundToken = findViewById(R.id.ditto_playground_token);
        playgroundToken.setText(String.format("Playground Token: %s", DITTO_PLAYGROUND_TOKEN));


        RecyclerView todoList = findViewById(R.id.todo_list);
        todoList.setLayoutManager(new LinearLayoutManager(this));

        todoAdapter = new TodoAdapter();
        todoList.setAdapter(todoAdapter);

        FloatingActionButton addButton = findViewById(R.id.add_button);
        addButton.setOnClickListener(v -> showAddTaskModal());

        todoAdapter.setOnTodoClickListener((todo, isChecked) -> {
            updateTodoList();
        });

        updateTodoList();
    }

    void requestPermissions() {
        DittoSyncPermissions permissions = new DittoSyncPermissions(this);
        String[] missing = permissions.missingPermissions(permissions.requiredPermissions());
        if (missing.length > 0) {
            this.requestPermissions(missing, 0);
        }
    }

    private void updateTodoList() {
        todoAdapter.setTodos(new ArrayList<>(tasks));
    }

    private void showAddTaskModal() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.modal_new_task, null);
        EditText modalTaskTitle = dialogView.findViewById(R.id.modal_task_title);

        builder.setView(dialogView)
                .setPositiveButton("Add", (dialog, which) -> {
                    String text = modalTaskTitle.getText().toString().trim();
                    if (!text.isEmpty()) {
                        tasks.add(new Task(text));
                        updateTodoList();
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        AlertDialog dialog = builder.create();
        dialog.show();

        // Show keyboard automatically
        modalTaskTitle.requestFocus();
        Objects.requireNonNull(dialog.getWindow()).setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
    }
}
