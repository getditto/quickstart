package live.ditto.quickstart.dittowrapper.aidl;

import live.ditto.quickstart.dittowrapper.aidl.QueryResult;
import live.ditto.quickstart.dittowrapper.aidl.IObserverCallback;

interface IDittoManager {

    boolean isSyncActive();

    void initDitto(String appId, String token, String customAuthUrl, String webSocketUrl);

    List<String> getMissingPermissions();

    void startSync();

    /**
     * Register a subscription with optional query arguments
     * @param subscriptionQuery DQL query string
     * @param args Query arguments as Bundle (can be null for no args)
     * @return UUID string to reference this subscription
     */
    String registerSubscription(String subscriptionQuery, in Bundle args);

    void closeSubscription(String uuid);

    void stopSync();

    QueryResult execute(String query, in Bundle args);

    /**
     * Register an observer with a callback for result updates
     * @param query DQL query string
     * @param args Query arguments as Bundle (can be null for no args)
     * @param callback Callback interface to receive query results
     * @return UUID string to reference this observer
     */
    String registerObserver(String query, in Bundle args, IObserverCallback callback);

    void closeObserver(String uuid);
}