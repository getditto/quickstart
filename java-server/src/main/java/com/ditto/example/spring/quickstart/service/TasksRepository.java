package com.ditto.example.spring.quickstart.service;

import com.ditto.java.Ditto;
import com.ditto.java.DittoException;
import com.ditto.java.DittoQueryResultItem;
import com.ditto.java.DittoStoreObserver;
import com.ditto.java.DittoSyncSubscription;
import com.ditto.java.serialization.DittoCborSerializable;

import jakarta.annotation.Nonnull;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component
public class TasksRepository {
    private static final String TASKS_COLLECTION_NAME = "tasks";

    private final DittoManager dittoManager;

    public TasksRepository(DittoManager dittoManager) {
        this.dittoManager = dittoManager;
    }

    public void addTask(@Nonnull String title) {
        try {
            dittoManager.getDitto().getStore().execute(
                    "INSERT INTO %s DOCUMENTS (:task)".formatted(TASKS_COLLECTION_NAME),
                    DittoCborSerializable.Dictionary.buildDictionary()
                            .put(
                                    "task",
                                    DittoCborSerializable.Dictionary.buildDictionary()
                                            .put("_id", UUID.randomUUID().toString())
                                            .put("title", title)
                                            .put("done", false)
                                            .put("deleted", false)
                                            .build()
                            )
                            .build()
            ).toCompletableFuture().join();
        } catch (Error e) {
            throw new RuntimeException(e);
        }
    }

    public void setTaskDone(@Nonnull String taskId, boolean done) {
        try {
            dittoManager.getDitto().getStore().execute(
                    "UPDATE %s SET done = :done WHERE _id = :id".formatted(TASKS_COLLECTION_NAME),
                    DittoCborSerializable.Dictionary.buildDictionary()
                            .put("done", done)
                            .put("id", taskId)
                            .build()
            ).toCompletableFuture().join();
        } catch (Error e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteTask(@Nonnull String taskId) {
        try {
            dittoManager.getDitto().getStore().execute(
                    "UPDATE %s SET deleted = true WHERE _id = :id".formatted(TASKS_COLLECTION_NAME),
                    DittoCborSerializable.Dictionary.buildDictionary()
                            .put("id", taskId)
                            .build()
            ).toCompletableFuture().join();
        } catch (Error e) {
            throw new RuntimeException(e);
        }
    }

    public void updateTask(@Nonnull String taskId, @Nonnull String newTitle) {
        try {
            dittoManager.getDitto().getStore().execute(
                    "UPDATE %s SET title = :title WHERE _id = :id".formatted(TASKS_COLLECTION_NAME),
                    DittoCborSerializable.Dictionary.buildDictionary()
                            .put("title", newTitle)
                            .put("id", taskId)
                            .build()
            ).toCompletableFuture().join();
        }  catch (Error e) {
            throw new RuntimeException(e);
        }
    }

    @Nonnull
    public Flux<List<Task>> observeAll() {
        // The subscription controls what syncs to this device and is written to
        // the local database; the observer reacts to changes in that local
        // database, hiding soft-deleted tasks from the returned list.
        final String subscriptionQuery = "SELECT * FROM %s".formatted(TASKS_COLLECTION_NAME);
        final String displayQuery =
            "SELECT * FROM %s WHERE NOT deleted".formatted(TASKS_COLLECTION_NAME);

        return Flux.create(emitter -> {
            Ditto ditto = dittoManager.getDitto();
            try {
                DittoSyncSubscription subscription = ditto.getSync().registerSubscription(subscriptionQuery);
                DittoStoreObserver observer = ditto.getStore().registerObserver(displayQuery, results ->
                    emitter.next(results.getItems().stream().map(this::itemToTask).toList()));

                emitter.onDispose(() -> {
                    // TODO: Can't just catch, this potentially leaks the `observer` resource.
                    try {
                        subscription.close();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    try {
                        observer.close();
                    } catch (DittoException e) {
                        throw new RuntimeException(e);
                    }
                });
            } catch (DittoException e) {
                emitter.error(e);
            }
        }, FluxSink.OverflowStrategy.LATEST);
    }

    private Task itemToTask(@Nonnull DittoQueryResultItem item) {
        DittoCborSerializable.Dictionary value = item.getValue();
        try {
            return new Task(
                    value.get("_id").asString(),
                    value.get("title").asString(),
                    value.get("done").asBoolean(),
                    value.get("deleted").asBoolean()
            );
        } catch (DittoException e) {
            throw new RuntimeException(e);
        }
    }
}
