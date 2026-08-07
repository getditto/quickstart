package com.example.dittotasks;

import android.app.Application;
import android.util.Log;

/**
 * Creates the process-global Ditto instance exactly once, at app startup, and starts
 * sync. Ditto lives for the whole process and is never tied to an Activity's lifecycle,
 * so a configuration change such as rotation recreates the Activity without recreating
 * Ditto (which would contend on the persistence-directory lock).
 */
public class TasksApplication extends Application {
    private static final String TAG = "TasksApplication";

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            DittoManager.initialize(
                    BuildConfig.DITTO_DATABASE_ID,
                    BuildConfig.DITTO_SERVER_URL,
                    BuildConfig.DITTO_DEVELOPMENT_TOKEN,
                    BuildConfig.DITTO_OFFLINE_LICENSE_TOKEN);
            DittoManager.startSync();
        } catch (Throwable ex) {
            Log.e(TAG, "Failed to initialize Ditto", ex);
            throw ex;
        }
    }
}
