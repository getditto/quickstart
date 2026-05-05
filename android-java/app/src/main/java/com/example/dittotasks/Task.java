package com.example.dittotasks;

import com.ditto.kotlin.DittoQueryResultItem;

import org.json.JSONException;
import org.json.JSONObject;

public class Task {
    private final String id;
    private final String title;
    private final boolean done;
    private final boolean deleted;

    public Task(String title) {
        this(null, title, false, false);
    }

    public Task(String id, String title, boolean done, boolean deleted) {
        this.id = id;
        this.title = title;
        this.done = done;
        this.deleted = deleted;
    }

    public static Task fromQueryItem(DittoQueryResultItem item) {
        try {
            JSONObject json = new JSONObject(item.jsonString());
            return new Task(
                    json.isNull("_id") ? null : json.optString("_id", null),
                    json.isNull("title") ? null : json.optString("title", null),
                    json.optBoolean("done", false),
                    json.optBoolean("deleted", false));
        } catch (JSONException e) {
            throw new RuntimeException("Failed to parse task from query result", e);
        }
    }

    public String getId() {
        return id;
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
