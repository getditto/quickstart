package com.example.dittotasks;

import java.util.Optional;

import live.ditto.DittoQueryResultItem;

public class Task {
    private Optional<String> id;
    private String title;
    private boolean done;
    private boolean deleted;

    public Task(String title) {
        this(null, title, false, false);
    }

    public Task(String id, String title, boolean done, boolean deleted) {
        this.id = Optional.ofNullable(id);
        this.title = title;
        this.done = done;
        this.deleted = deleted;
    }

    public static Task fromQueryItem(DittoQueryResultItem item) {
        var map = item.getValue();
        return new Task(
                (String) map.get("_id"),
                (String) map.get("title"),
                Boolean.TRUE.equals(map.get("done")),
                Boolean.TRUE.equals(map.get("deleted"))
        );
    }

    public String getTitle() {
        return title;
    }

    public boolean isDone() {
        return done;
    }

    public boolean isDeleted() {
        return deleted;
    }
}
