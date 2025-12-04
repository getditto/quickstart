package live.ditto.quickstart.dittowrapper;

import live.ditto.quickstart.dittowrapper.QueryResult;

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

}