package com.example.dittotasks;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.stream.Collectors;

import kotlin.Unit;
import live.ditto.Ditto;
import live.ditto.DittoDependencies;
import live.ditto.DittoError;
import live.ditto.DittoIdentity;
import live.ditto.DittoStoreObserver;
import live.ditto.DittoSyncSubscription;
import live.ditto.android.DefaultAndroidDittoDependencies;

public class DittoViewModel extends AndroidViewModel {
    private final TaskAdapter taskAdapter;
    private Ditto ditto;
    private DittoSyncSubscription taskSubscription;
    private DittoStoreObserver taskObserver;

    public static final String DITTO_APP_ID = "";
    public static final String DITTO_PLAYGROUND_TOKEN = "";

    public DittoViewModel(Application application) {
        super(application);
        this.taskAdapter = new TaskAdapter();
        initDitto();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        ditto.close();
    }

    private void initDitto() {
        try {
            DittoDependencies androidDependencies = new DefaultAndroidDittoDependencies(getApplication().getApplicationContext());
            var identity = new DittoIdentity.OnlinePlayground(androidDependencies, DITTO_APP_ID, DITTO_PLAYGROUND_TOKEN);
            ditto = new Ditto(androidDependencies, identity);
            ditto.disableSyncWithV3();
            ditto.startSync();

            taskSubscription = ditto.sync.registerSubscription("SELECT * FROM tasks");
            taskObserver = ditto.store.registerObserver("SELECT * FROM tasks WHERE deleted=false ORDER BY _id", null, result -> {
                var tasks = result.getItems().stream().map(Task::fromQueryItem).collect(Collectors.toCollection(ArrayList::new));
                taskAdapter.setTasks(new ArrayList<>(tasks));
                return Unit.INSTANCE;
            });
        } catch (DittoError e) {
            e.printStackTrace();
        }
    }

    public TaskAdapter getTaskAdapter() {
        return taskAdapter;
    }

    public void createTask(String title) {
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

    public void editTaskTitle(Task task, String newTitle) {
        HashMap<String, Object> args = new HashMap<>();
        args.put("id", task.getId());
        args.put("title", newTitle);

        try {
            ditto.store.execute("UPDATE tasks SET title=:title WHERE _id=:id", args);
        } catch (DittoError e) {
            e.printStackTrace();
        }
    }

    public void toggleTask(Task task) {
        HashMap<String, Object> args = new HashMap<>();
        args.put("id", task.getId());
        args.put("done", !task.isDone());

        try {
            ditto.store.execute("UPDATE tasks SET done=:done WHERE _id=:id", args);
        } catch (DittoError e) {
            e.printStackTrace();
        }
    }

    public void deleteTask(Task task) {
        HashMap<String, Object> args = new HashMap<>();
        args.put("id", task.getId());
        try {
            ditto.store.execute("UPDATE tasks SET deleted=true WHERE _id=:id", args);
        } catch (DittoError e) {
            e.printStackTrace();
        }
    }

    public void toggleSync() {
        if (ditto == null) {
            return;
        }

        boolean isSyncActive = ditto.isSyncActive();
        try {
            if (isSyncActive) {
                ditto.stopSync();
            } else {
                ditto.startSync();
            }
        } catch (DittoError e) {
            e.printStackTrace();
        }
    }
}
