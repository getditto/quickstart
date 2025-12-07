package com.ditto.example.spring.quickstart.service;

import com.ditto.java.*;
import com.ditto.java.serialization.DittoCborSerializable;
import jakarta.annotation.Nonnull;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component
public class DittoTaskService {

    private static final String TASKS_COLLECTION_NAME = "tasks";

    private final DittoService dittoService;

    public DittoTaskService(DittoService dittoService) {
        this.dittoService = dittoService;
    }

    public void addTask(@Nonnull String title) {
        try {
            DittoQueryResult result = dittoService
                .getDitto()
                .getStore()
                .execute(
                    "INSERT INTO %s DOCUMENTS (:newTask)".formatted(
                        TASKS_COLLECTION_NAME
                    ),
                    DittoCborSerializable.Dictionary.buildDictionary()
                        .put(
                            "newTask",
                            DittoCborSerializable.Dictionary.buildDictionary()
                                .put("_id", UUID.randomUUID().toString())
                                .put("title", title)
                                .put("done", false)
                                .put("deleted", false)
                                .build()
                        )
                        .build()
                )
                .toCompletableFuture()
                .join();
             result.close();
        } catch (Error | DittoException e) {
            throw new RuntimeException(e);
        }
    }

    public void toggleTaskDone(@Nonnull String taskId) {
        try {
            boolean isDone;
            try (DittoQueryResult tasks = dittoService
                .getDitto()
                .getStore()
                .execute(
                    "SELECT * FROM %s WHERE _id = :taskId".formatted(
                        TASKS_COLLECTION_NAME
                    ),
                    DittoCborSerializable.Dictionary.buildDictionary()
                        .put("taskId", taskId)
                        .build()
                )
                .toCompletableFuture()
                .join()
            ) {
                isDone = tasks
                    .getItems()
                    .get(0)
                    .getValue()
                    .get("done")
                    .asBoolean();
            }

            DittoQueryResult result = dittoService
                .getDitto()
                .getStore()
                .execute(
                    "UPDATE %s SET done = :done WHERE _id = :taskId".formatted(
                        TASKS_COLLECTION_NAME
                    ),
                    DittoCborSerializable.Dictionary.buildDictionary()
                        .put("done", !isDone)
                        .put("taskId", taskId)
                        .build()
                )
                .toCompletableFuture()
                .join();
            result.close();
        } catch (Error | DittoException e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteTask(@Nonnull String taskId) {
        try {
            DittoQueryResult result = dittoService
                .getDitto()
                .getStore()
                .execute(
                    "UPDATE %s SET deleted = :deleted WHERE _id = :taskId".formatted(
                        TASKS_COLLECTION_NAME
                    ),
                    DittoCborSerializable.Dictionary.buildDictionary()
                        .put("deleted", true)
                        .put("taskId", taskId)
                        .build()
                )
                .toCompletableFuture()
                .join();
            result.close();
        } catch (Error | DittoException e) {
            throw new RuntimeException(e);
        }
    }

    public void updateTask(@Nonnull String taskId, @Nonnull String newTitle) {
        try {
            DittoQueryResult result = dittoService
                .getDitto()
                .getStore()
                .execute(
                    "UPDATE %s SET title = :title WHERE _id = :taskId".formatted(
                        TASKS_COLLECTION_NAME
                    ),
                    DittoCborSerializable.Dictionary.buildDictionary()
                        .put("title", newTitle)
                        .put("taskId", taskId)
                        .build()
                )
                .toCompletableFuture()
                .join();
            result.close();
        } catch (Error| DittoException e) {
            throw new RuntimeException(e);
        }
    }

    @Nonnull
    public Flux<List<Task>> observeAll() {
        final String subscriptionQuery =
            "SELECT * FROM %s WHERE NOT deleted".formatted(TASKS_COLLECTION_NAME);
        final String displayQuery = subscriptionQuery + " ORDER BY title ASC";

        return Flux.create(
            emitter -> {
                Ditto ditto = dittoService.getDitto();
                try {
                    @SuppressWarnings("resource")
                    DittoSyncSubscription subscription = ditto
                        .getSync()
                        .registerSubscription(subscriptionQuery);

                    @SuppressWarnings("resource")
                    DittoStoreObserver observer = ditto
                        .getStore()
                        .registerObserver(displayQuery, results ->
                            emitter.next(
                                results
                                    .getItems()
                                    .stream()
                                    .map(item -> {
                                        try {
                                            return this.itemToTask(item);
                                        } catch (Exception e) {
                                            emitter.error(e);
                                            throw new RuntimeException(e);
                                        }
                                    })
                                    .toList()
                            )
                        );

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
            },
            FluxSink.OverflowStrategy.LATEST
        );
    }

    private Task itemToTask(@Nonnull DittoQueryResultItem item) throws DittoException {
        DittoCborSerializable.Dictionary value = item.getValue();
        return new Task(
            value.get("_id").asString(),
            value.get("title").asString(),
            value.get("done").asBoolean(),
            value.get("deleted").asBoolean()
        );
    }
}
