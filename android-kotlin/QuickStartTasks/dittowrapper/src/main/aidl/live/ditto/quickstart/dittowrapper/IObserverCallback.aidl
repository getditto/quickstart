package live.ditto.quickstart.dittowrapper;

interface IObserverCallback {
    /**
     * Called when the observer receives new results from Ditto
     * @param resultJson List of JSON strings representing the query results
     */
    void onResult(in List<String> resultJson);
}