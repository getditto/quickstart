package com.example.dittotasks;

import java.util.Optional;

import com.ditto.kotlin.DittoQueryResultItem;

import org.json.JSONException;
import org.json.JSONObject;

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
        try {
            JSONObject json = new JSONObject(item.jsonString());
            return new Task(
                    json.optString("_id", null),
                    json.optString("title", null),
                    json.optBoolean("done", false),
                    json.optBoolean("deleted", false));
        } catch (JSONException e) {
            throw new RuntimeException("Failed to parse task from query result", e);
        }
    }

    public String getId() {
        return id.orElse(null);
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
